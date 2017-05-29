package p2

import java.io.File

import breeze.linalg.DenseVector
import scalismo.common.PointId
import scalismo.io._
import scalismo.mesh.TriangleMesh
import scalismo.sampling.algorithms._
import scalismo.sampling.evaluators._
import scalismo.sampling.loggers._
import scalismo.sampling.proposals._
import scalismo.sampling._
import scalismo.statisticalmodel.asm._
import scalismo.ui.api.SimpleAPI.ScalismoUI

import scala.util.Random

/**
  * Main working class for second project, that is a model-based segmentation
  * of 5 CT femur images using MCMC methods and Active Shape Models.
  *
  * Created by George on 20/5/2017.
  */
object P2 {

  def main(args: Array[String]) {
    scalismo.initialize()
    val visualize = true



    val reconstructFemurNo = 1

    val targetName: String = reconstructFemurNo match {
      case 1 => "handedData/test/4"
      case 2 => "handedData/test/14"
      case 3 => "handedData/test/23"
      case 4 => "handedData/test/25"
      case 5 => "handedData/test/30"
    }


    val target: TriangleMesh = MeshIO.readMesh(new File(targetName+".stl")).get

    val asm: ActiveShapeModel = ActiveShapeModelIO.readActiveShapeModel(new File("handedData/femur-asm.h5")).get
    val image = ImageIO.read3DScalarImage[Short](new File(targetName+".nii")).get.map(_.toFloat)

    val preProcessedGradientImage: PreprocessedImage = asm.preprocessor(image)


    println("Defining the Markov chain...")
    implicit val random = new Random(1)

    val lowVarianceGenerator = GaussianProposal(asm.statisticalModel.rank, 0.001f)
    val verylowVarianceGenerator = GaussianProposal(asm.statisticalModel.rank, 0.01f)
    val largeVarianceGenerator =  GaussianProposal(asm.statisticalModel.rank, 0.2f)

    val lowScaleGenerator = ScaleProposal(0.1f)
    val midScaleGenerator = ScaleProposal(0.2f)
    val highScaleGenerator = ScaleProposal(0.5f)
    val randomWalkGenerator = MixtureProposal.fromProposalsWithTransition((0.8, lowVarianceGenerator),(0.05,
      largeVarianceGenerator), (0.15, verylowVarianceGenerator))
    val scaleGenerator = MixtureProposal.fromProposalsWithTransition((0.8, lowScaleGenerator), (0.1,
      midScaleGenerator), (0.1, highScaleGenerator))

    val mixtureGenerator = MixtureProposal.fromProposalsWithTransition((0.7, randomWalkGenerator), (0.3,
      scaleGenerator))

    val likelihoodEvaluator = CorrespondenceEvaluator(asm, preProcessedGradientImage)
    val priorEvaluator = ShapePriorEvaluator(asm)
    val posteriorEvaluator = ProductEvaluator(priorEvaluator, likelihoodEvaluator)

    val chain = MetropolisHastings(randomWalkGenerator, posteriorEvaluator , logger)

    val initialParameters = ShapeParameters(DenseVector.zeros[Float](asm.statisticalModel.rank))
    //val initialParameters = ShapeParameters(asm.statisticalModel.coefficients(asm.statisticalModel.mean))

    println("Sampling from the chain...")
    val metropolisHastingsIterator = chain.iterator(initialParameters)
    val samplingIterator = for (theta <- metropolisHastingsIterator) yield {
      //ui.setCoefficientsOf("model", theta.modelCoefficients)
      //println(theta)
      theta
    }
    val time1 = System.currentTimeMillis()

    val samples = samplingIterator.drop(200).take(500)

    val bestSample = samples.maxBy(posteriorEvaluator.logValue)
    if(visualize){
      val ui = ScalismoUI()
      ui.show(target, "groundtruthFemur")
      ui.show(asm.statisticalModel, "model")
      //ui.show(preProcessedGradientImage, "image")
      ui.setCoefficientsOf("model", bestSample.modelCoefficients)
    }
    val bestModel = asm.statisticalModel.instance(bestSample.modelCoefficients)
    val diff = bestModel.pointIds.map{
      id : PointId => (target.point(id) - bestModel.point(id)).norm
     }.sum

    println("Done.")
    println(s"#Accepted: ${countAccepted.toString} #Rejected: ${countRejected.toString}")
    println(bestSample.modelCoefficients)
    println(s"Diff: ${diff}")
    println(s"Time needed: ${System.currentTimeMillis() - time1}")

  }


}
