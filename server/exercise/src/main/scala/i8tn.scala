package com.eigengo.lift.exercise.i8tn

/**
 * Created by charlesrice on 26/01/15.
 */

trait i8tn {
  def appName: String
  def okCaption: String

  /**
   * Arms localization
   */
  def exerciseArms: String
  def exerciseArmsBicepCurl: String
  def exerciseArmsHammerCurl: String
  def exerciseArmsPronatedCurl: String
  def exerciseArmsDip: String
  def exericseArmsTricepPushdown: String
  def exerciseArmsTricepOverheadExtension: String
  def exerciseArmsCloseGripBenchPress: String

  /**
   * Back localization
   */
  def exerciseBack: String
  def exerciseBackDeadlift: String
  def exerciseBackPullup: String
  def exeriseBackRow: String
  def exerciseBackHyperExtension: String

  /**
   * Chest localization
   */
  def exerciseChest: String
  def exerciseChestChestPress: String
  def exerciseChestButterfly: String
  def exerciseChestCableCrossover: String
  def exerciseChestInclinePress: String
  def exerciseChestPushup: String

  /**
   * Core localization
   */
  def exerciseCore: String
  def exerciseCoreCrunch: String
  def exerciseCoreSideBend: String
  def exerciseCoreCableCrunch: String
  def exerciseCoreSitup: String
  def exerciseCoreLegRaises: String

  /**
   * Legs localization
   */
  def exerciseLegs: String
  def exerciseLegsSquat: String
  def exerciseLegsLegPress: String
  def exerciseLegsLegExtension: String
  def exerciseLegsLegCurl: String
  def exerciseLegsLunge: String

  /**
   * Shoulders localization
   */
  def exerciseShoulders: String
  def exerciseShouldersShoulderPress: String
  def exerciseShouldersLateralRaise: String
  def exerciseShouldersFrontRaise: String
  def exerciseShouldersRearRaise: String
  def exerciseShouldersUprightRow: String
  def exerciseShouldersShrug: String

  /**
   * Cardiovascular localization
   */
  def exerciseCardio: String
  def exerciseCardioRunning: String
  def exerciseCardioCycling: String
  def exerciseCardioSwimming: String
  def exerciseCardioElliptical: String
  def exerciseCardioRowing: String
}

object English extends i8tn {
  val appName = "Lift"
  val okCaption = "OK"

  /**
   * Arms localization
   */
  val exerciseArms = "Arms"
  val exerciseArmsBicepCurl = "Bicep curl"
  val exerciseArmsHammerCurl = "Hammer curl"
  val exerciseArmsPronatedCurl = "Pronated curl"
  val exerciseArmsDip = "Dip"
  val exericseArmsTricepPushdown = "Tricep push down"
  val exerciseArmsTricepOverheadExtension = "Tricep overhead extension"
  val exerciseArmsCloseGripBenchPress = "Close-grip bench press"

  /**
   * Back localization
   */
  val exerciseBack = "Back"
  val exerciseBackDeadlift = "Deadlift"
  val exerciseBackPullup = "Pull up"
  val exeriseBackRow = "Row"
  val exerciseBackHyperExtension = "Hyper-extension"

  /**
   * Chest localization
   */
  val exerciseChest = "Chest"
  val exerciseChestChestPress = "Chest press"
  val exerciseChestButterfly = "Butterfly"
  val exerciseChestCableCrossover = "Cable crossover"
  val exerciseChestInclinePress = "Incline chest press"
  val exerciseChestPushup = "Push up"

  /**
   * Core localization
   */
  val exerciseCore = "Core"
  val exerciseCoreCrunch = "Crunch"
  val exerciseCoreSideBend = "Side bend"
  val exerciseCoreCableCrunch = "Cable crunch"
  val exerciseCoreSitup = "Sit up"
  val exerciseCoreLegRaises = "Leg raises"

  /**
   * Legs localization
   */
  val exerciseLegs = "Legs"
  val exerciseLegsSquat = "Squat"
  val exerciseLegsLegPress = "Leg press"
  val exerciseLegsLegExtension = "Leg extension"
  val exerciseLegsLegCurl = "Leg curl"
  val exerciseLegsLunge = "Lunge"

  /**
   * Shoulders localization
   */
  val exerciseShoulders = "Shoulders"
  val exerciseShouldersShoulderPress = "Shoulder press"
  val exerciseShouldersLateralRaise = "Lateral raise"
  val exerciseShouldersFrontRaise = "Front raise"
  val exerciseShouldersRearRaise = "Rear raise"
  val exerciseShouldersUprightRow = "Upright row"
  val exerciseShouldersShrug = "Shrug"

  /**
   * Cardiovascular localization
   */
  val exerciseCardio = "Cardiovascular"
  val exerciseCardioRunning = "Running"
  val exerciseCardioCycling = "Cycling"
  val exerciseCardioSwimming = "Swimming"
  val exerciseCardioElliptical = "Elliptical"
  val exerciseCardioRowing = "Rowing"

}

object Localized {
  @volatile private var currenti8tn: i8tn = English

  def apply(f: i8tn ⇒ String): String = f(currenti8tn)

  def setLanguage(l: i8tn): Unit = (currenti8tn = l)

}
