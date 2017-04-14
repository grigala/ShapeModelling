package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.numerics.RandomMeshSampler3D
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI

object SimpleICP {

  def main(args: Array[String]) {

    ////////////////////SETTINGS FOR ICP
    val numIterations = 20
    val noise = NDimensionalNormalDistribution(Vector(0, 0, 0), SquareMatrix((1f, 0, 0), (0, 1f, 0), (0, 0, 1f)))

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/aligned/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)
    val referenceShapeModel = new File("data/reference_shape_model.h5")
    /////////////////////////////////////////////////////////////////

    val refFemur = MeshIO.readMesh(refFile).get
    val model = StatismoIO.readStatismoMeshModel(referenceShapeModel).get

    //val target = MeshIO.readMesh(new File("data/aligned/meshes/femur_0.stl")).get
    //val transformationField: DiscreteVectorField[_3D, _3D] =
    //  calcCorrespondentTransformationsICP(model, target, noise, numIterations)
    println("Calculate transformation fields from data with ICP...")
    val dataset = files.map { f: File => MeshIO.readMesh(f).get }
    val transformationFields : IndexedSeq[DiscreteVectorField[_3D, _3D]] = dataset.map{ targetMesh =>
      calcCorrespondentTransformationsICP(model, targetMesh, noise, numIterations)
    }
    println("Calculate Statistical Mesh Model from data...")
    val continuousFields = transformationFields.map(f => f.interpolateNearestNeighbor)
    val dataGP = DiscreteLowRankGaussianProcess.createUsingPCA(refFemur, continuousFields)
    val dataModel = StatisticalMeshModel(refFemur, dataGP.interpolateNearestNeighbor)
    ui.show(dataModel, "dataShapeModel")

    StatismoIO.writeStatismoMeshModel(dataModel, new File("data/data_shape_model.h5"))


    /*
    println("augment kernel..")
    //val customKernel = gp.cov
    val augmentedKernel = dataGP.interpolateNearestNeighbor.cov + model.gp.interpolateNearestNeighbor.cov
    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))
    val sampler = RandomMeshSampler3D(refFemur, 300, 42)
    println("Do lowrank approximation...")
    val gp = GaussianProcess(zeroMean, augmentedKernel)
    val lowRankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 30)
    val finalModel = StatisticalMeshModel(refFemur, lowRankGP)

    ui.show(finalModel, "augmented model")
    //ui.show(transformationField, "deformations")
    */
    print("done")
  }
}