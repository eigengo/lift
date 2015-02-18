import Foundation

struct Notification {

  struct Notification {
    var alert: String
    var category: String
    var title: String?
  }

  struct AmbiguousExerciseNotification {
    var notification: Notification
    var exercises: [Exercise.Exercise]
  }

}