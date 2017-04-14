package ch.unibas.cs.shm.ex2

import java.io.File

import scalismo.common.{DiscreteVectorField, RealSpace, VectorField}
import scalismo.geometry.{Point, Vector, _3D}
import scalismo.io.MeshIO
import scalismo.kernels.{DiagonalKernel, GaussianKernel, MatrixValuedPDKernel, PDKernel}
import scalismo.mesh.Mesh
import scalismo.numerics.{GradientDescentOptimizer, RandomMeshSampler3D}
import scalismo.registration._
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, StatisticalMeshModel}
import scalismo.ui.api.SimpleAPI.ScalismoUI

/**
  * Created by George on 3/4/2017.
  */
object Launcher {

  def main(args: Array[String]): Unit = {

    scalismo.initialize()
    val ui = ScalismoUI()

    /* Part 2 */

    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")

    val femur_ref = MeshIO.readMesh(refFile).get

    ui.show(femur_ref, "femur_ref")

    val zeroMean = VectorField(RealSpace[_3D], (pt:Point[_3D]) => Vector(0,0,0))

    def transformFromDefField(defField : DiscreteVectorField[_3D, _3D]) =   (pt : Point[_3D]) => {
      pt+defField(femur_ref.findClosestPoint(pt).id)
    }


    ui.remove("femur_warped")
    ui.remove("gaussianKernelGP_sample")

    val l = 100
    //val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](50, 50, 350)

    println("Oh boy, if you survive this then it should be ok...")
    val svk1 = GaussianKernel[_3D](l) * s(0)
    val svk2 = GaussianKernel[_3D](l) * s(1)
    val svk3 = GaussianKernel[_3D](l) * s(2)
    val matrixValuedKernel = DiagonalKernel(svk1, svk2, svk3)

    val gp = GaussianProcess(zeroMean, matrixValuedKernel)

    println("Start sampling...")
    val sampler = RandomMeshSampler3D(femur_ref, 200, 42)
    val lowrankGP : LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp, sampler, 100)
    val sample = lowrankGP.sampleAtPoints(femur_ref)
    println("Showing samples: " + sample.toString())
    //ui.show(sample, "gaussianKernelGP_sample")



    println("Applying transformation...")
    val transform =  transformFromDefField(sample)
    //ui.show(femur_ref.transform(transformFromDefField(sample)), "femur_warped")


    println("Rendering statistical mesh model...")
    val model = StatisticalMeshModel(femur_ref, lowrankGP)
    ui.show(model, "femur_model")
    println("Launch process ended")

    val targetMesh = MeshIO.readMesh(new File("data/aligned/meshes/femur_0.stl")).get
    val meshView = ui.show(targetMesh, "targetMesh")

    println("sampling again")
    val evaluationSampler = RandomMeshSampler3D(femur_ref, 1000, 42)

    println("registration")
    val regConfig = RegistrationConfiguration(
      optimizer = GradientDescentOptimizer(numIterations = 50, stepLength = 0.1),
      metric = MeanSquaresMetric(evaluationSampler),
      transformationSpace = GaussianProcessTransformationSpace(lowrankGP),
      regularizationWeight = 1e-8,
      regularizer = L2Regularizer
    )

    val fixedImage = Mesh.meshToDistanceImage(femur_ref)
    val movingImage = Mesh.meshToDistanceImage(targetMesh)

    println("iteration")
    val regIterator = Registration.iterations(regConfig)(fixedImage, movingImage)

    val resultIterator = for ((it, itnum) <- regIterator.zipWithIndex) yield {
      println(s"object value in iteration $itnum is ${it.optimizerState.value}")
      it.registrationResult
    }
    println("done")
  }
}
