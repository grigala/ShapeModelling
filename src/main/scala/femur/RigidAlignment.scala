package femur

import java.io.File

import scalismo.geometry._
import scalismo.io._
import scalismo.registration.LandmarkRegistration
import scalismo.geometry.Landmark


object RigidAlignment {

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // create a visualization window
    //val ui = ScalismoUI()

    /////////////////////Load Files
    val files = new File("data/SMIR/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/SMIR/landmarks/").listFiles().take(50)

    /////////////////////////////////////////////////////////////////


    val dataset = files.map { f: File => MeshIO.readMesh(f).get }
    val refData = MeshIO.readMesh(refFile).get
    // show ref femur bone
    //ui.show(refData, "femur_ref")


    // load landmarks, add them to femurs
    val landmarks = landmFiles.map { f: File => LandmarkIO.readLandmarksJson[_3D](f).get }
    val refLandmark = LandmarkIO.readLandmarksJson[_3D](refLandmFile).get

    val bestTransforms = (0 until 50).map { i: Int => LandmarkRegistration.rigid3DLandmarkRegistration(landmarks(i), refLandmark) }

    val alignedLandmarks = (bestTransforms zip landmarks).map { case (bTrans, landmarks) =>
      (0 until landmarks.length).map { i: Int => new Landmark("L_" + i, bTrans(landmarks(i).point)) }
    }

    val alignedFemurs = (0 until 50).map { i: Int => dataset(i).transform(bestTransforms(i)) }

    (0 until 50).foreach { i: Int => LandmarkIO.writeLandmarksJson(alignedLandmarks(i), new File("data/aligned/landmarks/femur_" +i+".json")) }
    (0 until 50).foreach { i: Int => MeshIO.writeSTL(alignedFemurs(i), new File("data/aligned/meshes/femur_" + i + ".stl")) }

    print("done")
  }
}