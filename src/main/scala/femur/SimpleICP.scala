package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI

object SimpleICP {

  def main(args: Array[String]) {

    ////////////////////SETTINGS FOR ICP
    val numIterations = 15
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
    println("Calculate transformation fields from data...")
    val dataset = files.map { f: File => MeshIO.readMesh(f).get }
    val transformationFields : IndexedSeq[DiscreteVectorField[_3D, _3D]] = dataset.map{ targetMesh =>
      calcCorrespondentTransformationsICP(model, targetMesh, noise, numIterations)
    }
    println("Calculate Statistical Mesh Model from data...")
    val continuousFields = transformationFields.map(f => f.interpolateNearestNeighbor)
    val dataGP = DiscreteLowRankGaussianProcess.createUsingPCA(refFemur, continuousFields)
    val finalModel = StatisticalMeshModel(refFemur, dataGP.interpolateNearestNeighbor)
    ui.show(finalModel, "dataShapeModel")

    //ui.show(transformationField, "deformations")

    print("done")
  }
}