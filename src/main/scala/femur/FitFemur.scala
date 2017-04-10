package femur

import java.io.File

import scalismo.common._
import scalismo.geometry._
import scalismo.io._
import scalismo.kernels._
import scalismo.numerics._
import scalismo.registration.{LandmarkRegistration, Transformation}
import scalismo.statisticalmodel._
import scalismo.ui.api.SimpleAPI.ScalismoUI
import scalismo.geometry.Landmark
import scalismo.statisticalmodel.dataset.{DataCollection, DataItem}


object FitFemur {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/SMIR/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/aligned/landmarks/").listFiles().take(50)

    val femur_ref = MeshIO.readMesh(refFile).get

    /////////////////////////////////////////////////////////////////


    val dataSet = files.map { f: File => MeshIO.readMesh(f).get }
    val numPoints = dataSet.map(m => m.pointIds.length)
    numPoints.foreach(i => println(i))

     val defFields :IndexedSeq[DiscreteVectorField[_3D,_3D]] = dataSet.map{ m =>
      val deformationVectors = femur_ref.pointIds.map{ id : PointId =>
        m.findClosestPoint(femur_ref.point(id)).point - femur_ref.point(id)
      }.toIndexedSeq

      DiscreteVectorField(femur_ref, deformationVectors)
    }
    val continuousFields = defFields.map(f => f.interpolateNearestNeighbor )
    val gp = DiscreteLowRankGaussianProcess.createUsingPCA(femur_ref, continuousFields)
    val model = StatisticalMeshModel(femur_ref, gp.interpolateNearestNeighbor)
    ui.show(model, "model")
    //val refData = MeshIO.readMesh(refFile).get
    // show ref femur bone
    //ui.show(refData, "femur_ref")



    //(0 until 50).foreach { i: Int => ui.show(alignedFemurs(i), "femur_" + i) }
    /*
      val defFields :IndexedSeq[DiscreteVectorField[_3D,_3D]] = alignedFemurs.map{ m =>
        val deformationVectors = refData.pointIds.map{ id : PointId =>
          m.point(id) - refData.point(id)
        }.toIndexedSeq

        DiscreteVectorField(refData, deformationVectors)
      }

      val continuousFields = defFields.map(f => f.interpolateNearestNeighbor )
      val gp = DiscreteLowRankGaussianProcess.createUsingPCA(refData, continuousFields)

      val model = StatisticalMeshModel(refData, gp.interpolateNearestNeighbor)
      ui.show(model, "data_model")
  */

    //////////////////////////////////////////////////////////////
    /*
    val femur_ref = MeshIO.readMesh(refFile).get

    val zeroMean = VectorField(RealSpace[_3D], (pt: Point[_3D]) => Vector(0, 0, 0))

    val l = 80
    val scalarValuedKernel = GaussianKernel[_3D](l) * 1
    val s = Array[Double](10, 10, 250)
    val matrixValuedKernel = DiagonalKernel(scalarValuedKernel * s(0), scalarValuedKernel * s(1), scalarValuedKernel * s(2))

    val gp2 = GaussianProcess(zeroMean, matrixValuedKernel)

    val sampler = RandomMeshSampler3D(femur_ref, 500, 42)
    val lowrankGP: LowRankGaussianProcess[_3D, _3D] = LowRankGaussianProcess.approximateGP(gp2, sampler, 30)


    val model = StatisticalMeshModel(femur_ref, lowrankGP)

    // show ref femur bone
    ui.show(femur_ref, "femur_ref")
    // show statistical model with custom kernel
    ui.show(model, "model")

  */
    print("done")
  }
}