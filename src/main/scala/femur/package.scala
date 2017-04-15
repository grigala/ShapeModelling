import scalismo.common.{DiscreteVectorField, PointId, UnstructuredPointsDomain}
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.numerics.UniformMeshSampler3D
import scalismo.statisticalmodel._

/**
  * Created by Fabricio on 14.04.2017.
  */
package object femur {

  def calcCorrespondentTransformationsICP(model: StatisticalMeshModel, target: TriangleMesh, noise:
  NDimensionalNormalDistribution[_3D], numIterations: Int): DiscreteVectorField[_3D, _3D] = {

    val pointSamples = UniformMeshSampler3D(model.mean, 7000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map { s => model.mean.findClosestPoint(s).id }


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
      val fit = fitModel(pointIds, candidates)
      val newPoints = pointIds.map(id => fit.point(id))
      if (nbIterations > 0) {
        //Thread.sleep(3000)
        recursion(newPoints, nbIterations - 1)
      } else {
        //ui.remove("fit")
        //ui.show(fit,"fit")
        newPoints
      }
    }


    val fittedModelPoints = recursion(pointIds.map(id => model.mean.point(id)), numIterations)
    val targetPoints = attributeCorrespondences(fittedModelPoints)
    val refPoints = pointIds.map(id => model.mean.point(id))

    val domain = UnstructuredPointsDomain[_3D](refPoints.toIndexedSeq)
    val diff = refPoints.zip(targetPoints).map { case (mPt, pPt) => pPt - mPt }

    DiscreteVectorField(domain, diff.toIndexedSeq)
  }

}
