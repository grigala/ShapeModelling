package p2

import java.io.File

import breeze.linalg.DenseVector
import scalismo.common.PointId
import scalismo.io._
import scalismo.mesh.TriangleMesh
import scalismo.sampling.algorithms._
import scalismo.sampling.evaluators._
import scalismo.sampling.proposals._
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


    val reconstructFemurNo = 7

    val targetName: String = reconstructFemurNo match {
      //test with groundtruth
      case 1 => "handedData/test/4"
      case 2 => "handedData/test/14"
      case 3 => "handedData/test/23"
      case 4 => "handedData/test/25"
      case 5 => "handedData/test/30"
      //targets with no groundtruth
      case 6 => "handedData/targets/1"
      case 7 => "handedData/targets/9"
      case 8 => "handedData/targets/10"
      case 9 => "handedData/targets/13"
      case 10 => "handedData/targets/37"
    }


    val asm: ActiveShapeModel = ActiveShapeModelIO.readActiveShapeModel(new File("handedData/femur-asm.h5")).get
    val image = ImageIO.read3DScalarImage[Short](new File(targetName+".nii")).get.map(_.toFloat)

    val preProcessedGradientImage: PreprocessedImage = asm.preprocessor(image)


    println("Defining the Markov chain...")
    implicit val random = scala.util.Random

    val largeVarianceGenerator =  GaussianProposal(asm.statisticalModel.rank, 0.2f)
    val lowVarianceGenerator = GaussianProposal(asm.statisticalModel.rank, 0.01f)
    val verylowVarianceGenerator = GaussianProposal(asm.statisticalModel.rank, 0.001f)
    val verylowVarianceGenerator2 = GaussianProposal(asm.statisticalModel.rank, 0.05f)


    val randomWalkGenerator = MixtureProposal.fromSymmetricProposalsWithTransition((0.4, lowVarianceGenerator),(0.2,
      largeVarianceGenerator), (0.2, verylowVarianceGenerator), (0.2, verylowVarianceGenerator2))

    //val sparseGenerator1 = SparseGaussianProposal(asm.statisticalModel.rank, 3, 0.1f)
    val sparseGenerators : List[(Double, SparseGaussianProposal)] = List.tabulate(asm.statisticalModel.rank)(n =>
      ((1.0/asm.statisticalModel.rank.toDouble), SparseGaussianProposal(asm.statisticalModel.rank, n, 0.1f)))
    val sparseWalkGenerator = MixtureProposal.fromSymmetricProposalsWithTransition(sparseGenerators:_*)

    val mixtureGenerator = MixtureProposal.fromSymmetricProposalsWithTransition((0.5, randomWalkGenerator), (0.5, sparseWalkGenerator))
    val likelihoodEvaluator = CorrespondenceEvaluator(asm, preProcessedGradientImage)
    val priorEvaluator = ShapePriorEvaluator(asm)
    val posteriorEvaluator = ProductEvaluator(priorEvaluator, likelihoodEvaluator)


    val chain = MetropolisHastings(mixtureGenerator, posteriorEvaluator , logger)

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

    val samples = samplingIterator.take(5000)

    val bestSample = samples.maxBy(posteriorEvaluator.logValue)


    val bestModel = asm.statisticalModel.instance(bestSample.modelCoefficients)
    println("Done.")


    val ui = ScalismoUI()
    ui.show(asm.statisticalModel, "model")
    //ui.show(preProcessedGradientImage, "image")
    ui.setCoefficientsOf("model", bestSample.modelCoefficients)

    println(s"#Accepted: ${countAccepted.toString} #Rejected: ${countRejected.toString}")
    println(bestSample.modelCoefficients)
    if(reconstructFemurNo<=5){
      val target: TriangleMesh = MeshIO.readMesh(new File(targetName+".stl")).get
      ui.show(target, "groundtruthFemur")
      val diff = bestModel.pointIds.map{
        id : PointId => (target.point(id) - bestModel.point(id)).norm
      }.sum
      println(s"Diff: ${diff}")
    }
    println(s"Time needed: ${System.currentTimeMillis() - time1}")

  }


}
