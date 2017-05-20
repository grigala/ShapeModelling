package p2

import java.io.File

import scalismo.common.PointId
import scalismo.geometry.{Landmark, Point, _3D}
import scalismo.io.{LandmarkIO, MeshIO, StatismoIO}
import scalismo.mesh.TriangleMesh
import scalismo.sampling.algorithms.MetropolisHastings
import scalismo.sampling.evaluators.ProductEvaluator
import scalismo.statisticalmodel.StatisticalMeshModel
import scalismo.ui.api.SimpleAPI.ScalismoUI

import scala.collection.immutable

/**
  * Main working class for second project, that is a model-based segmentation
  * of 5 CT femur images using MCMC methods and Active Shape Models.
  *
  * Created by George on 20/5/2017.
  */
object P2 {

  scalismo.initialize()

  val ui = ScalismoUI()

  // Statistical mesh model ?which?
  val model: StatisticalMeshModel = StatismoIO.readStatismoMeshModel(new File("datasets/.h5")).get
  // Target mesh we are willing to segmentate ?which? target.stl?
  val target: TriangleMesh = MeshIO.readMesh(new File("datasets/.stl")).get

  val modelLandmarks: immutable.Seq[Landmark[_3D]] = LandmarkIO.readLandmarksJson[_3D](new File("datasets/")).get
  val targetLandmarks: immutable.Seq[Landmark[_3D]] = LandmarkIO.readLandmarksJson[_3D](new File("datasets/")).get

  ui.show(target, "target")
  ui.show(model, "model")
  ui.addLandmarksTo(modelLandmarks, "model")
  ui.addLandmarksTo(targetLandmarks, "target")

  val modelLandmkarIds: immutable.Seq[PointId] = modelLandmarks.map(l => model.mean.pointId(l.point).get)
  val targetPoints: immutable.Seq[Point[_3D]] = targetLandmarks.map(l => l.point)
  val correspondences: immutable.Seq[(PointId, Point[_3D])] = modelLandmkarIds.zip(targetPoints)

  val generator = GaussianProposal(model.rank, 0.1f)
  val likelihoodEvaluator = CorrespondenceEvaluator(model, correspondences, 0.1f)
  val priorEvaluator = ShapePriorEvaluator(model)
  val posteriorEvaluator = ProductEvaluator(priorEvaluator, likelihoodEvaluator)
  val chan = MetropolisHastings(generator, posteriorEvaluator)

}
