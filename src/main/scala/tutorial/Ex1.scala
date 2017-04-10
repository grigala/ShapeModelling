package tutorial

import java.awt.Color
import java.io.File

import scalismo.geometry._3D
import scalismo.io.{LandmarkIO, MeshIO}
import scalismo.registration.LandmarkRegistration
import scalismo.ui.api.SimpleAPI.ScalismoUI

object Ex1{

  def main(args: Array[String]) {

    // required to initialize native libraries (VTK, HDF5 ..)
    scalismo.initialize()

    // Your application code goes below here. Below is a dummy application that reads a mesh and displays it

    // create a visualization window
    val ui = ScalismoUI()

    val files = new File("data/SMIR/meshes/").listFiles().take(50)
    val refFile = new File("data/femur.stl")
    val refLandmFile = new File("data/femur.json")
    val landmFiles = new File("data/SMIR/landmarks/").listFiles().take(50)

    val dataset = files.map{f: File => MeshIO.readMesh(f).get}
    val refData = MeshIO.readMesh(refFile).get
    // show ref femur bone
    ui.show(refData, "femur_ref")


    // load landmarks, add them to femurs
    val landmarks = landmFiles.map{f: File => LandmarkIO.readLandmarksJson[_3D](f).get}
    val refLandmark = LandmarkIO.readLandmarksJson[_3D](refLandmFile).get

    val bestTransforms = (0 until 50).map{i: Int => LandmarkRegistration.rigid3DLandmarkRegistration(landmarks(i), refLandmark)}
    val alignedFemurs = (0 until 50).map{i: Int => dataset(i).transform(bestTransforms(i))}

    (0 until 50).foreach{i: Int => ui.show(alignedFemurs(i), "femur_"+i)}

  }
}
