package tutorial

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels._
import scalismo.numerics._
import scalismo.registration.LandmarkRegistration
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI


object Ex2{

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////////////////////////////////////////////////
    val refFile = new File("data/femur.stl")
    val femur_ref = MeshIO.readMesh(refFile).get

    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))

    val l = 80
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](10, 10, 250)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val gp = GaussianProcess(zeroMean, matrixValuedKernel)

    val sampler = RandomMeshSampler3D(femur_ref, 500, 42)
    val lowrankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 30)


    val model = StatisticalMeshModel(femur_ref, lowrankGP)

    // show ref femur bone
    ui.show(femur_ref, "femur_ref")
    // show statistical model with custom kernel
    ui.show(model, "model")


    //def transformFromDefField(defField : DiscreteVectorField[_3D, _3D]) =   (pt : Point[_3D]) => {
    //  pt+defField(femur_ref.findClosestPoint(pt).id)
    //}
    //val sample = lowrankGP.sampleAtPoints(femur_ref)
    //show(sample, "gaussianKernelGP_sample")
    //val transform =  transformFromDefField(sample)
    //show(femur_ref.transform(transformFromDefField(sample)), "femur_warped")
  }
}

/*


remove("femur_warped")
remove("gaussianKernelGP_sample")

val l : Double = 50.0
val s : Double = 150.0

val scalarValuedKernel = GaussianKernel[_3D](l) * s 
val matrixValuedKernel = DiagonalKernel[_3D](scalarValuedKernel)

val gp = GaussianProcess(zeroMean, matrixValuedKernel)

val sampler = RandomMeshSampler3D(femur_ref, 200, 42)
val lowrankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 30) 
val sample = lowrankGP.sampleAtPoints(femur_ref)
show(sample, "gaussianKernelGP_sample")




val transform =  transformFromDefField(sample)
show(femur_ref.transform(transformFromDefField(sample)), "femur_warped")

///////////////

*/

