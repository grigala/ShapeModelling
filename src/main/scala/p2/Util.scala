package p2


import breeze.linalg.{DenseMatrix, DenseVector, diag}
import breeze.stats.distributions.Gaussian
import scalismo.geometry.{Point, SquareMatrix, Vector3D, _3D}
import scalismo.sampling.{DistributionEvaluator, ProposalGenerator, SymmetricTransition, TransitionProbability}
import scalismo.statisticalmodel.asm.{ActiveShapeModel, PreprocessedImage}
import scalismo.statisticalmodel.{MultivariateNormalDistribution, NDimensionalNormalDistribution, StatisticalMeshModel}

/**
  * Porting 15th tutorial Code as a helper class
  *
  * Created by George on 19/5/2017.
  */


case class ShapeParameters(modelCoefficients: DenseVector[Float])

case class GaussianProposal(paramVectorSize: Int, stdev: Float) extends
  ProposalGenerator[ShapeParameters] with TransitionProbability[ShapeParameters] with SymmetricTransition[ShapeParameters] {

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(paramVectorSize),
    DenseMatrix.eye[Float](paramVectorSize) * stdev)

  val scaleDistr = new MultivariateNormalDistribution(DenseVector.zeros(1),
    DenseMatrix.eye[Float](1) * stdev)

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

case class ScaleProposal(stdev: Float) extends
  ProposalGenerator[ShapeParameters] with TransitionProbability[ShapeParameters] with SymmetricTransition[ShapeParameters] {

  val scaleDistr = new MultivariateNormalDistribution(DenseVector.zeros(1),
    DenseMatrix.eye[Float](1) * stdev)

  override def propose(theta: ShapeParameters): ShapeParameters = {
    val scale = scaleDistr.sample()
    val thetaPrime = ShapeParameters(theta.modelCoefficients * scale.valueAt(0))
    thetaPrime
  }


  override def logTransitionProbability(from: ShapeParameters, to: ShapeParameters) = {
    val residual = to.modelCoefficients - from.modelCoefficients
    scaleDistr.logpdf(residual.slice(1,2))
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

    val likelihoods = for (id <- ids) yield {
      val profile = model.profiles(id)
      val profilePointOnMesh = currModelInstance.point(profile.pointId)
      val featureAtPoint = model.featureExtractor(preprocessedGradientImage, profilePointOnMesh, currModelInstance, profile.pointId).get
      profile.distribution.logpdf(featureAtPoint)
    }
    val loglikelihood = likelihoods.sum
    loglikelihood
  }
}

case class ProximityEvaluator(model: StatisticalMeshModel, targetLandmarks: Seq[Point[_3D]],
                              sdev: Double = 1.0) extends DistributionEvaluator[ShapeParameters] {

  val uncertainty = NDimensionalNormalDistribution(Vector3D(0f, 0f, 0f), SquareMatrix.eye[_3D] * (sdev * sdev))

  override def logValue(theta: ShapeParameters): Double = {

    val currModelInstance = model.instance(theta.modelCoefficients)

    val likelihoods = targetLandmarks.map { targetLandmark =>
      val closestPointCurrentFit = currModelInstance.findClosestPoint(targetLandmark).point
      val observedDeformation = targetLandmark - closestPointCurrentFit
      uncertainty.logpdf(observedDeformation)
    }

    val loglikelihood = likelihoods.sum
    loglikelihood
  }
}


