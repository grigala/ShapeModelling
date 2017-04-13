package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels._
import scalismo.numerics._
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI

object ReferenceShapeModel {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 10
    val noise = NDimensionalNormalDistribution(Vector(0,0,0), SquareMatrix((1f,0,0), (0,1f,0), (0,0,1f)))


    // create a visualization window
    //val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/aligned/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)
    /////////////////////////////////////////////////////////////////

    val refFemur = MeshIO.readMesh(refFile).get

    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))

    val l = 80
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](20, 20, 250)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val gp = GaussianProcess(zeroMean, matrixValuedKernel)
    val sampler = RandomMeshSampler3D(refFemur, 400, 42)
    val lowRankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 40)
    val model = StatisticalMeshModel(refFemur, lowRankGP)
    //ui.show(model, "model")
    StatismoIO.writeStatismoMeshModel(model, new File("data/reference_shape_model.h5"))
    print("done")
  }
}