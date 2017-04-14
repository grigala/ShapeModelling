/**
  * Created by Fabricio on 14.04.2017.
  */

package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io.{MeshIO, StatismoIO}
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.numerics.RandomMeshSampler3D
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI

object AugmentShapeModel {

  def main(args: Array[String]): Unit = {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    val ui = ScalismoUI()
    ///////load files
    val dataShapeModel = new File("data/data_shape_model.h5")
    val referenceShapeModel = new File("data/reference_shape_model.h5")
    val refFile = new File("data/femur.stl")
    ////////////////////////////////////
    println("read Models...")
    val refFemur = MeshIO.readMesh(refFile).get
    val dataModel = StatismoIO.readStatismoMeshModel(dataShapeModel).get
    //val model = StatismoIO.readStatismoMeshModel(referenceShapeModel).get
    println("create custom kernel...")
    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))
    val l = 100
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](15, 15, 250)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val kernelGP = GaussianProcess(zeroMean, matrixValuedKernel)

    println("augment kernel..")
    //val customKernel = gp.cov
    val augmentedKernel = dataModel.gp.interpolateNearestNeighbor.cov + kernelGP.cov
    val sampler = RandomMeshSampler3D(refFemur, 200, 42)
    println("Do lowrank approximation...")
    val gp = GaussianProcess(zeroMean, augmentedKernel)
    val lowRankGP: LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 20)
    val finalModel = StatisticalMeshModel(refFemur, lowRankGP)

    ui.show(finalModel, "augmented model")
    //ui.show(transformationField, "deformations")

  }

}