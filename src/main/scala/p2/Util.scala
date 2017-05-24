package p2


import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.common.PointId
import scalismo.geometry.{Point, SquareMatrix, Vector3D, _3D}
import scalismo.sampling.{DistributionEvaluator, ProposalGenerator, SymmetricTransition, TransitionProbability}
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

case class ShapePriorEvaluator(model: StatisticalMeshModel) extends DistributionEvaluator[ShapeParameters] {
  override def logValue(theta: ShapeParameters): Double = {
    model.gp.logpdf(theta.modelCoefficients)
  }
}

case class CorrespondenceEvaluator(model: StatisticalMeshModel, correspondences: Seq[(PointId, Point[_3D])],
                                   tolerance: Double) extends DistributionEvaluator[ShapeParameters] {

  val uncertainty = NDimensionalNormalDistribution(Vector3D(0f, 0f, 0f), SquareMatrix.eye[_3D] * tolerance)

  override def logValue(theta: ShapeParameters): Double = {

    val currModelInstance = model.instance(theta.modelCoefficients)
    val likelihoods = correspondences.map { case (id, targetPoint) =>
      val modelInstancePoint = currModelInstance.point(id)
      val observedDeformation = targetPoint - modelInstancePoint

      uncertainty.logpdf(observedDeformation)
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

case class PoseAndShapeParameters(rotationParameters: DenseVector[Float], translationParameters: DenseVector[Float], modelCoefficients: DenseVector[Float])


case class ShapeUpdateProposal(paramVectorSize: Int, stdev: Float) extends
  ProposalGenerator[PoseAndShapeParameters] with TransitionProbability[PoseAndShapeParameters] with SymmetricTransition[PoseAndShapeParameters] {

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(paramVectorSize),
    DenseMatrix.eye[Float](paramVectorSize) * stdev)


  override def propose(theta: PoseAndShapeParameters): PoseAndShapeParameters = {
    val perturbation = perturbationDistr.sample()
    val thetaPrime = PoseAndShapeParameters(theta.rotationParameters, theta.translationParameters, theta.modelCoefficients + perturbationDistr.sample)
    thetaPrime
  }

  override def logTransitionProbability(from: PoseAndShapeParameters, to: PoseAndShapeParameters) = {
    val residual = to.modelCoefficients - from.modelCoefficients
    perturbationDistr.logpdf(residual)
  }

}

case class RotationUpdateProposal(stdev: Float) extends
  ProposalGenerator[PoseAndShapeParameters] with TransitionProbability[PoseAndShapeParameters] with SymmetricTransition[PoseAndShapeParameters] {

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(3),
    DenseMatrix.eye[Float](3) * stdev)

  def propose(theta: PoseAndShapeParameters): PoseAndShapeParameters = {
    PoseAndShapeParameters(theta.rotationParameters + perturbationDistr.sample, theta.translationParameters, theta.modelCoefficients)
  }

  override def logTransitionProbability(from: PoseAndShapeParameters, to: PoseAndShapeParameters) = {
    val residual = to.rotationParameters - from.rotationParameters
    perturbationDistr.logpdf(residual)
  }
}

case class TranslationUpdateProposal(stdev: Float) extends
  ProposalGenerator[PoseAndShapeParameters] with TransitionProbability[PoseAndShapeParameters] with SymmetricTransition[PoseAndShapeParameters] {

  val perturbationDistr = new MultivariateNormalDistribution(DenseVector.zeros(3),
    DenseMatrix.eye[Float](3) * stdev)

  def propose(theta: PoseAndShapeParameters): PoseAndShapeParameters = {
    PoseAndShapeParameters(theta.rotationParameters, theta.translationParameters + perturbationDistr.sample, theta.modelCoefficients)
  }

  override def logTransitionProbability(from: PoseAndShapeParameters, to: PoseAndShapeParameters) = {
    val residual = to.translationParameters - from.translationParameters
    perturbationDistr.logpdf(residual)
  }
}
