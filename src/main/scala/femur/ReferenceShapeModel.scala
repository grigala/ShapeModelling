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
    RigidAlignment.rigidAlignment()

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

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
    val numEigenvectors = 30
    val l = 100
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](10, 10, 200)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val gp = GaussianProcess(zeroMean, matrixValuedKernel)
    println("Sample from Shape Model...")
    val sampler = RandomMeshSampler3D(refFemur, 500, 42)
    println("Do lowrank approximation...")
    val lowRankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, numEigenvectors)

    val model = StatisticalMeshModel(refFemur, lowRankGP)
    //ui.show(model, "model")
    StatismoIO.writeStatismoMeshModel(model, new File("data/reference_shape_model.h5"))
    println("done")
  }
}