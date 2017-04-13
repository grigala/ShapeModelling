package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels._
import scalismo.numerics._
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI
import scalismo.mesh.{Mesh, TriangleMesh}
object SimpleICP {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    ////////////////////SETTINGS FOR ICP
    val numIterations = 10
    val noise = NDimensionalNormalDistribution(Vector(0,0,0), SquareMatrix((1f,0,0), (0,1f,0), (0,0,1f)))

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/aligned/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)
    val referenceShapeModel = new File("data/reference_shape_model.h5")
    /////////////////////////////////////////////////////////////////

    val model = StatismoIO.readStatismoMeshModel(referenceShapeModel).get
    ui.show(model, "model")

    val target = MeshIO.readMesh(new File("data/aligned/meshes/femur_0.stl")).get
    //val targetSampler = RandomMeshSampler3D(target, 500, 42)
    ui.show(target,"target")

    val pointSamples = UniformMeshSampler3D(model.mean, 5000, 42).sample.map(s => s._1)
    val pointIds = pointSamples.map{s => model.mean.findClosestPoint(s).id}

    def attributeCorrespondences(pts : Seq[Point[_3D]]) : Seq[Point[_3D]] = {
      pts.map{pt => target.findClosestPoint(pt).point}
    }

    def fitModel(pointIds: IndexedSeq[PointId],candidateCorresp: Seq[Point[_3D]]) :TriangleMesh = {
      val trainingData = (pointIds zip candidateCorresp).map{ case (mId, pPt) =>
        (mId, pPt, noise)
      }
      val posterior = model.posterior(trainingData.toIndexedSeq)
      posterior.mean
    }

    def recursion(currentPoints : Seq[Point[_3D]], nbIterations : Int) : Unit= {

      val candidates = attributeCorrespondences(currentPoints)
      val fit = fitModel(pointIds, candidates)
      ui.remove("fit")
      ui.show(fit,"fit")

      val newPoints= pointIds.map(id => fit.point(id))

      if(nbIterations> 0) {
        //Thread.sleep(3000)
        recursion(newPoints, nbIterations - 1)
      }
    }

    recursion( pointIds.map(id => model.mean.point(id)) , numIterations)

    print("done")
  }
}