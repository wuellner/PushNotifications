//
//  AppDelegate+notification.m
//  pushtest
//
//  Created by Robert Easterday on 10/26/12.
//
//

#import "AppDelegate+notification.h"
#import <objc/runtime.h>

static char launchNotificationKey;

@implementation AppDelegate (notification)

// its dangerous to override a method from within a category.
// Instead we will use method swizzling. we set this up in the load call.
+ (void)load
{
    Method original, swizzled;

    original = class_getInstanceMethod(self, @selector(init));
    swizzled = class_getInstanceMethod(self, @selector(swizzled_init));
    method_exchangeImplementations(original, swizzled);
}

- (AppDelegate *)swizzled_init
{
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(createNotificationChecker:)
               name:@"UIApplicationDidFinishLaunchingNotification" object:nil];

	// This actually calls the original init method over in AppDelegate. Equivilent to calling super
	// on an overrided method, this is not recursive, although it appears that way. neat huh?
	return [self swizzled_init];
}

// This code will be called immediately after application:didFinishLaunchingWithOptions:. We need
// to process notifications in cold-start situations
- (void)createNotificationChecker:(NSNotification *)notification
{
	if (notification) {
		NSDictionary *launchOptions = [notification userInfo];
        if (launchOptions){
			
            NSMutableDictionary* notification = [NSMutableDictionary dictionaryWithDictionary:
                                                 [launchOptions objectForKey: @"UIApplicationLaunchOptionsRemoteNotificationKey"]];
            
            [notification setObject:[NSNumber numberWithBool:NO] forKey:@"foreground"];
            
            [notification setObject:[NSNumber numberWithBool:YES] forKey:@"userAction"];
            
            [[NotificationService instance] receivedNotification:notification];
            
        }
	}
}

-(void)application:(UIApplication *)application didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings {

    [[NotificationService instance] didRegisterUserNotificationSettings:notificationSettings];
    
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    
    [[NotificationService instance] onRegistered:deviceToken];
   
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    
    [[NotificationService instance] failToRegister:error];

}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    // Get application state for iOS4.x+ devices, otherwise assume active
    UIApplicationState appState = UIApplicationStateActive;
    if ([application respondsToSelector:@selector(applicationState)]) {
        appState = application.applicationState;
    }

    NSMutableDictionary* notification = [NSMutableDictionary dictionaryWithDictionary:[userInfo objectForKey:@"aps"]];
    
    if (appState == UIApplicationStateActive) {
        [notification setObject:[NSNumber numberWithBool:YES] forKey:@"foreground"];
    }
    else{
        [notification setObject:[NSNumber numberWithBool:NO] forKey:@"foreground"];
        [notification setObject:[NSNumber numberWithBool:YES] forKey:@"coldstart"];
    }
    
    [[NotificationService instance] receivedNotification:notification];
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    NSLog(@"active");
    //zero badge
    application.applicationIconBadgeNumber = 0;
}

@end
