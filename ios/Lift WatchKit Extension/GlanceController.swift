//
//  GlanceController.swift
//  Lift WatchKit Extension
//
//  Created by Jan Machacek on 12/16/14.
//  Copyright (c) 2014 Jan Machacek. All rights reserved.
//

import WatchKit
import Foundation


class GlanceController: WKInterfaceController {
    @IBOutlet var tableView: WKInterfaceTable!

    override func awakeWithContext(context: AnyObject?) {
        super.awakeWithContext(context)
        
        tableView.setNumberOfRows(3, withRowType: "set")
    }

    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
        NSLog("%@ will activate", self)
    }

    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        NSLog("%@ did deactivate", self)
        super.didDeactivate()
    }

}
