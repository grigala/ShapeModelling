package femur

import java.io.File

import breeze.linalg.norm
import scalismo.common.{DiscreteVectorField, PointId, UnstructuredPointsDomain}
import scalismo.geometry._
import scalismo.io._
import scalismo.mesh.TriangleMesh
import scalismo.numerics.UniformMeshSampler3D
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI


object FitFemur {

  def main(args: Array[String]) {

    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 20
    val noise = NDimensionalNormalDistribution(Vector(0, 0, 0), SquareMatrix((1f, 0, 0), (0, 1f, 0), (0, 0, 1f)))

    val ui = ScalismoUI()

    println("Loading and displaying partial mesh...")
    val target: TriangleMesh = MeshIO.readMesh(new File("data/partials/VSD.Right_femur.XX.XX.OT.101148.0.stl")).get
    ui.show(target, "partialShape")

    println("Loading and displaying statistical shape model...")
    val model: StatisticalMeshModel = StatismoIO.readStatismoMeshModel(new File("data/augmented_shape_model.h5")).get
    //ui.show(model, "model")


    val pointSamples = UniformMeshSampler3D(model.mean, 5000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map { s => model.mean.findClosestPoint(s).id }
    val p = Point3D(-40.558f, 26.1689f, -208.722f)
    val correctedPointIds = pointIds.filter{ id : PointId =>   (model.referenceMesh.point(id) - p).norm > 62}
    //ui.show(correctedPointIds.map{id => model.mean.point(id)}, "points")

    def attributeCorrespondences(pts: Seq[Point[_3D]]): Seq[Point[_3D]] = {
      pts.map { pt => target.findClosestPoint(pt).point }
    }

    def fitModel(pointIds: IndexedSeq[PointId], candidateCorresp: Seq[Point[_3D]]): TriangleMesh = {
      val trainingData = (pointIds zip candidateCorresp).map { case (mId, pPt) =>
        (mId, pPt, noise)
      }
      val posterior = model.posterior(trainingData.toIndexedSeq)
      posterior.mean
    }

    def recursion(currentPoints: Seq[Point[_3D]], nbIterations: Int): Seq[Point[_3D]] = {

      val candidates = attributeCorrespondences(currentPoints)
      val fit = fitModel(correctedPointIds, candidates)
      val newPoints = correctedPointIds.map(id => fit.point(id))
      if (nbIterations > 0) {
        //Thread.sleep(1000)

        recursion(newPoints, nbIterations - 1)
      } else {
        ui.remove("fit")
        ui.show(fit,"fit")
        newPoints
      }
    }


    println("Performing fitting recursion and calculating correspondences...")
    val fittedModelPoints = recursion(correctedPointIds.map(id => model.mean.point(id)), numIterations)
    val targetPoints = attributeCorrespondences(fittedModelPoints)
    val refPoints = correctedPointIds.map(id => model.mean.point(id))

    val domain = UnstructuredPointsDomain[_3D](refPoints.toIndexedSeq)
    val diff = refPoints.zip(targetPoints).map { case (mPt, pPt) => pPt - mPt }

    DiscreteVectorField(domain, diff.toIndexedSeq)


    print("done")

  }
}