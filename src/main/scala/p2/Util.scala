package p2


import java.util.NoSuchElementException

import breeze.linalg._
import breeze.stats.distributions.Gaussian
import scalismo.sampling.{DistributionEvaluator, ProposalGenerator, SymmetricTransition, TransitionProbability}
import scalismo.statisticalmodel.asm.{ActiveShapeModel, PreprocessedImage}
import scalismo.statisticalmodel.MultivariateNormalDistribution

/**
  * Created by George on 19/5/2017.
  */


case class ShapeParameters(modelCoefficients: DenseVector[Float])

case class GaussianProposal(paramVectorSize: Int, stdev: Float) extends
  ProposalGenerator[ShapeParameters] with TransitionProbability[ShapeParameters] with SymmetricTransition[ShapeParameters] {

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(paramVectorSize),
    DenseMatrix.eye[Float](paramVectorSize) * stdev)

  override def propose(theta: ShapeParameters): ShapeParameters = {
    val perturbation = perturbationDistr.sample()
    val thetaPrime = ShapeParameters(theta.modelCoefficients + perturbation)
    thetaPrime
  }


  override def logTransitionProbability(from: ShapeParameters, to: ShapeParameters) = {
    val residual = to.modelCoefficients - from.modelCoefficients
    perturbationDistr.logpdf(residual)
  }
}

case class SparseGaussianProposal(paramVectorSize: Int, position:Int, stdev: Float) extends
  ProposalGenerator[ShapeParameters] with TransitionProbability[ShapeParameters] with SymmetricTransition[ShapeParameters] {

  require(position<paramVectorSize)

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(1),
    DenseMatrix.eye[Float](1) * stdev)

  override def propose(theta: ShapeParameters): ShapeParameters = {
    val perturbation = perturbationDistr.sample()
    val perturbationVector = DenseVector.zeros[Float](paramVectorSize)
    perturbationVector(position) = perturbation.valueAt(0)
    val thetaPrime = ShapeParameters(theta.modelCoefficients + perturbationVector)
    thetaPrime
  }


  override def logTransitionProbability(from: ShapeParameters, to: ShapeParameters) = {
    val residual = to.modelCoefficients - from.modelCoefficients
    perturbationDistr.logpdf(DenseVector.zeros[Float](1)+residual.valueAt(position))
  }
}


/**
  * Prior probability
  *
  * @param model ActiveShapeModel
  */
case class ShapePriorEvaluator(model: ActiveShapeModel) extends DistributionEvaluator[ShapeParameters] {
  override def logValue(theta: ShapeParameters): Double = {
    model.statisticalModel.gp.logpdf(theta.modelCoefficients)
  }
}

/**
  * Computes Likelihood of intensities
  *
  * @param model ActiveShapeModel
  */
case class CorrespondenceEvaluator(model: ActiveShapeModel, preprocessedGradientImage : PreprocessedImage) extends
  DistributionEvaluator[ShapeParameters] {

  override def logValue(theta: ShapeParameters): Double = {

    val currModelInstance = model.statisticalModel.instance(theta.modelCoefficients)
    val ids = model.profiles.ids
    try{
      val likelihoods = for (id <- ids) yield {
        val profile = model.profiles(id)
        val profilePointOnMesh = currModelInstance.point(profile.pointId)
        val featureAtPoint = model.featureExtractor(preprocessedGradientImage, profilePointOnMesh, currModelInstance, profile.pointId).get
        profile.distribution.logpdf(featureAtPoint)

      }
      likelihoods.sum
    }catch {
      case e:NoSuchElementException => -breeze.numerics.inf
    }

  }
}

