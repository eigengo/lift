import Foundation

extension UIViewController {
    
    func setBackgroundImage(imageNames: [String]) {
        if (imageNames.count == 0) {
            return
        }
        
        self.view.layoutIfNeeded()

        let imageName = imageNames[random() % imageNames.count]
        let image = UIImage(named: imageName)
        let backgroundImage = UIImageView(image: image)
        backgroundImage.alpha = 0.3
        backgroundImage.contentMode = UIViewContentMode.ScaleAspectFill
        backgroundImage.frame = self.view.frame
        self.view.addSubview(backgroundImage)
        self.view.sendSubviewToBack(backgroundImage)
    }
    
}
