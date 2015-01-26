package com.eigengo.lift.exercise.i8tn

/**
 * Created by charlesrice on 26/01/15.
 */

trait i8tn {
  def appName: String
  def okCaption: String

  /**
   * Arms localization
   * @return
   */
  def exerciseArms: String
  def exerciseArmsBicepCurl: String

  /**
   * Chest localization
   */
  def exerciseChest: String
  def exerciseChestChestPress: String

}

object English extends i8tn {
  val appName = "Lift"
  val okCaption = "OK"

  /**
   * Arms localization
   */
  val exerciseArms = "Arms"
  val exerciseArmsBicepCurl = "Bicep curl"

  /**
   * Chest localization
   */
  val exerciseChest = "Chest"
  val exerciseChestChestPress = "Chest press"

}

object Localized {
  @volatile private var currenti8tn: i8tn = English

  def apply(f: i8tn ⇒ String): String = f(currenti8tn)

  def setLanguage(l: i8tn): Unit = (currenti8tn = l)

}