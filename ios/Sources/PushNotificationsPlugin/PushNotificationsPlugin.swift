import Foundation
import Capacitor
import UserNotifications

enum PushNotificationError: Error {
    case tokenParsingFailed
    case tokenRegistrationFailed
}

enum PushNotificationsPermissions: String {
    case prompt
    case denied
    case granted
}

@objc(PushNotificationsPlugin)
public class PushNotificationsPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "PushNotificationsPlugin"
    public let jsName = "PushNotifications"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "register", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unregister", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDeliveredNotifications", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeAllDeliveredNotifications", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "removeDeliveredNotifications", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "createChannel", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "listChannels", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteChannel", returnType: CAPPluginReturnPromise)
    ]
    static let pendingNotificationKey = "PushNotificationsPendingNotification"
    static let lastFiredNotificationDateKey = "PushNotificationsLastFiredDate"

    private let notificationDelegateHandler = PushNotificationsHandler()
    private let acknowledgeService = AcknowledgeService()
    private var appDelegateRegistrationCalled: Bool = false
    private var isFirstActive = true
    var lastTapDate: Date?

    override public func load() {
        self.bridge?.notificationRouter.pushNotificationHandler = self.notificationDelegateHandler
        self.notificationDelegateHandler.plugin = self
        self.acknowledgeService.setContext(self.getConfig().getString("apiUrl"))

        NotificationCenter.default.addObserver(self,
                                            selector: #selector(self.onBackgroundNotification(notification:)),
                                            name: NSNotification.Name("silentNotificationReceived"),
                                            object: nil)

        NotificationCenter.default.addObserver(self,
                                            selector: #selector(self.didRegisterForRemoteNotificationsWithDeviceToken(notification:)),
                                            name: .capacitorDidRegisterForRemoteNotifications,
                                            object: nil)

        NotificationCenter.default.addObserver(self,
                                            selector: #selector(self.didFailToRegisterForRemoteNotificationsWithError(notification:)),
                                            name: .capacitorDidFailToRegisterForRemoteNotifications,
                                            object: nil)

        NotificationCenter.default.addObserver(self,
                                            selector: #selector(self.onAppBecomeActive),
                                            name: UIApplication.didBecomeActiveNotification,
                                            object: nil)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    /**
     * Register for push notifications
     */
    @objc func register(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
        call.resolve()
    }

    /**
     * Unregister for remote notifications
     */
    @objc func unregister(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            UIApplication.shared.unregisterForRemoteNotifications()
            call.resolve()
        }
    }

    /**
     * Request notification permission
     */
    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        self.notificationDelegateHandler.requestPermissions { granted, error in
            guard error == nil else {
                if let err = error {
                    call.reject(err.localizedDescription)
                    return
                }

                call.reject("unknown error in permissions request")
                return
            }

            var result: PushNotificationsPermissions = .denied

            if granted {
                result = .granted
            }

            call.resolve(["receive": result.rawValue])
        }
    }

    /**
     * Check notification permission
     */
    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        self.notificationDelegateHandler.checkPermissions { status in
            var result: PushNotificationsPermissions = .prompt

            switch status {
            case .notDetermined:
                result = .prompt
            case .denied:
                result = .denied
            case .ephemeral, .authorized, .provisional:
                result = .granted
            @unknown default:
                result = .prompt
            }

            call.resolve(["receive": result.rawValue])
            self.acknowledgeService.setNotificationsEnabled(result == .granted ? true : false)
        }
    }

    /**
     * Get notifications in Notification Center
     */
    @objc func getDeliveredNotifications(_ call: CAPPluginCall) {
        if !appDelegateRegistrationCalled {
            call.reject("event capacitorDidRegisterForRemoteNotifications not called.  Visit https://capacitorjs.com/docs/apis/push-notifications for more information")
            return
        }
        UNUserNotificationCenter.current().getDeliveredNotifications(completionHandler: { (notifications) in
            let ret = notifications.map({ (notification) -> [String: Any] in
                return self.notificationDelegateHandler.makeNotificationRequestJSObject(notification.request)
            })
            call.resolve([
                "notifications": ret
            ])
        })
    }

    /**
     * Remove specified notifications from Notification Center
     */
    @objc func removeDeliveredNotifications(_ call: CAPPluginCall) {
        if !appDelegateRegistrationCalled {
            call.reject("event capacitorDidRegisterForRemoteNotifications not called.  Visit https://capacitorjs.com/docs/apis/push-notifications for more information")
            return
        }
        guard let notifications = call.getArray("notifications", JSObject.self) else {
            call.reject("Must supply notifications to remove")
            return
        }

        let ids = notifications.map { $0["id"] as? String ?? "" }
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ids)
        call.resolve()
    }

    /**
     * Remove all notifications from Notification Center
     */
    @objc func removeAllDeliveredNotifications(_ call: CAPPluginCall) {
        if !appDelegateRegistrationCalled {
            call.reject("event capacitorDidRegisterForRemoteNotifications not called.  Visit https://capacitorjs.com/docs/apis/push-notifications for more information")
            return
        }
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        DispatchQueue.main.async(execute: {
            UIApplication.shared.applicationIconBadgeNumber = 0
        })
        call.resolve()
    }

    @objc func createChannel(_ call: CAPPluginCall) {
        call.unimplemented("Not available on iOS")
    }

    @objc func deleteChannel(_ call: CAPPluginCall) {
        call.unimplemented("Not available on iOS")
    }

    @objc func listChannels(_ call: CAPPluginCall) {
        call.unimplemented("Not available on iOS")
    }

    @objc private func onAppBecomeActive() {
        let actionId = isFirstActive ? "start" : "resume"
        isFirstActive = false

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            if let tapDate = self.lastTapDate, Date().timeIntervalSince(tapDate) < 1.0 {
                self.lastTapDate = nil
                return
            }
            self.firePendingNotificationIfNeeded(actionId: actionId)
        }
    }

    private func firePendingNotificationIfNeeded(actionId: String) {
        guard getConfig().getBoolean("fireActionOnResume", false) else { return }

        let defaults = UserDefaults.standard

        // Path 1: silent/data notification stored by onBackgroundNotification (content-available: 1)
        if let json = defaults.string(forKey: PushNotificationsPlugin.pendingNotificationKey),
           let jsonData = json.data(using: .utf8),
           let notificationDict = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] {
            defaults.removeObject(forKey: PushNotificationsPlugin.pendingNotificationKey)
            fireActionPerformed(actionId: actionId, notification: JSTypes.coerceDictionaryToJSObject(notificationDict) ?? JSObject())
            return
        }

        // Path 2: display notification delivered to the notification center while app was in background
        let lastFiredDate = Date(timeIntervalSince1970: defaults.double(forKey: PushNotificationsPlugin.lastFiredNotificationDateKey))

        UNUserNotificationCenter.current().getDeliveredNotifications { notifications in
            guard let latest = notifications
                .filter({ $0.date > lastFiredDate })
                .sorted(by: { $0.date > $1.date })
                .first(where: {
                    let userInfo = JSTypes.coerceDictionaryToJSObject($0.request.content.userInfo) ?? JSObject()
                    return (userInfo["notificationTitle"] as? String).map { !$0.isEmpty } ?? false
                }) else { return }

            DispatchQueue.main.async {
                defaults.set(Date().timeIntervalSince1970, forKey: PushNotificationsPlugin.lastFiredNotificationDateKey)
                let notificationData = self.notificationDelegateHandler.makeNotificationRequestJSObject(latest.request)
                self.fireActionPerformed(actionId: actionId, notification: notificationData)
            }
        }
    }

    func fireActionPerformed(actionId: String, notification: JSObject) {
        var actionData = JSObject()
        actionData["actionId"] = actionId
        actionData["notification"] = notification
        self.notifyListeners("pushNotificationActionPerformed", data: actionData, retainUntilConsumed: true)
    }

    @objc public func onBackgroundNotification(notification: Notification) {
        NSLog("Receive background notification")
        let data = notification.userInfo as? [String : Any] ?? ["something": "happened"]
        let event: [String: Any] = [
            "data": data
        ]

        let state = UIApplication.shared.applicationState
        if state == .active {
            self.notifyListeners("silentNotificationReceived", data: event, retainUntilConsumed: true);
        }

        if state != .active,
           let title = data["notificationTitle"] as? String, !title.isEmpty,
           let jsonData = try? JSONSerialization.data(withJSONObject: ["data": data]),
           let json = String(data: jsonData, encoding: .utf8) {
            UserDefaults.standard.set(json, forKey: PushNotificationsPlugin.pendingNotificationKey)
            NSLog("PushNotificationsPlugin: stored pending notification")
        }

        self.acknowledgeService.newNotification(data);
    }

    @objc public func didRegisterForRemoteNotificationsWithDeviceToken(notification: NSNotification) {
        appDelegateRegistrationCalled = true
        if let deviceToken = notification.object as? Data {
            let deviceTokenString = deviceToken.reduce("", {$0 + String(format: "%02X", $1)})
            notifyListeners("registration", data: [
                "value": deviceTokenString
            ])
        } else if let stringToken = notification.object as? String {
            notifyListeners("registration", data: [
                "value": stringToken
            ])
        } else {
            notifyListeners("registrationError", data: [
                "error": PushNotificationError.tokenParsingFailed.localizedDescription
            ])
        }
    }

    @objc public func didFailToRegisterForRemoteNotificationsWithError(notification: NSNotification) {
        appDelegateRegistrationCalled = true
        guard let error = notification.object as? Error else {
            return
        }
        notifyListeners("registrationError", data: [
            "error": error.localizedDescription
        ])
    }
}
