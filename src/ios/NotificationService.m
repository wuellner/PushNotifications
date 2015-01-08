//
//  NotificationService.m
//  AppGyver
//
//  Created by Rafael Almeida on 19/12/14.
//  Copyright (c) 2014 AppGyver Inc. All rights reserved.
//

#import "NotificationService.h"

static NotificationService *instance;

@interface PluginReference : NSObject

@property (strong, nonatomic) CDVInvokedUrlCommand* registerCallBack;

@property (strong, nonatomic) CDVInvokedUrlCommand* foregroundMessageCallBack;

@property (strong, nonatomic) CDVInvokedUrlCommand* backgroundMessageCallBack;

@property (strong, nonatomic) NSMutableArray* listOfNotificationsDelivered;

@property (strong, nonatomic) CDVPlugin* plugin;

@property (nonatomic) BOOL notifiedOfRegistration;

@end

@implementation PluginReference

@synthesize registerCallBack, foregroundMessageCallBack, backgroundMessageCallBack, plugin, listOfNotificationsDelivered;

-(void) cleanUp {
    [listOfNotificationsDelivered removeAllObjects];

    plugin = nil;
    registerCallBack = nil;
    foregroundMessageCallBack = nil;
    backgroundMessageCallBack = nil;
}

-(void)notifyRegistration{
    if(registerCallBack == nil){
        return;
    }

    if(![[NotificationService instance] notificationsEnabled]){
        CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Push notifications are not enabled for this app."];
        [plugin.commandDelegate sendPluginResult:commandResult callbackId:registerCallBack.callbackId];
    }
    else if([NotificationService instance].deviceToken){
        CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[NotificationService instance].deviceToken];
        [plugin.commandDelegate sendPluginResult:commandResult callbackId:registerCallBack.callbackId];
    }
    else if([NotificationService instance].failedToRegisterError){

        CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                           messageAsString:[NSString stringWithFormat:@"Error during registration: %@",
                                                                            [NotificationService instance].failedToRegisterError]];
        [plugin.commandDelegate sendPluginResult:commandResult callbackId:registerCallBack.callbackId];
    }
}

-(void)sendNotification:(NSDictionary*)notification toCallback:(CDVInvokedUrlCommand*)callback {
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:notification];
    commandResult.keepCallback = [NSNumber numberWithBool:YES];
    [plugin.commandDelegate sendPluginResult:commandResult callbackId:callback.callbackId];

}

-(void)sendNotification:(NSDictionary*)notification {

    if(!listOfNotificationsDelivered){
        listOfNotificationsDelivered = [NSMutableArray new];
    }

    if([listOfNotificationsDelivered containsObject:notification]){
        //NSLog(@"Notification already delivered to this webview");
        return;
    }

    BOOL isForeground = [[notification valueForKey:@"foreground"] boolValue];

    if(isForeground){
        [self sendNotification:notification toCallback:foregroundMessageCallBack];
    }
    else{
        [self sendNotification:notification toCallback:backgroundMessageCallBack];
    }

    [listOfNotificationsDelivered addObject:notification];
}

@end


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation NotificationService

@synthesize deviceToken, listOfPluginReferences, listOfNotifications, failedToRegisterError;

+(NotificationService*) instance {
    if ( instance == nil ){
        instance = [NotificationService new];
    }
    return instance;
}

- (id) init {
    self = [super init];
    if(self){
        listOfPluginReferences = [NSMutableArray new];
        listOfNotifications = [NSMutableArray new];
    }
    return self;
}

-(void)cleanUp {
    [listOfNotifications removeAllObjects];
    [listOfPluginReferences removeAllObjects];
}

-(void)failToRegister:(NSError*)error{
    failedToRegisterError = error.description;
    [self notifyRegistrationToAllWebViews];
}

-(void) unRegister {
    [[UIApplication sharedApplication] unregisterForRemoteNotifications];
    deviceToken = nil;
    [self cleanUp];
}

-(bool) notificationsEnabled {
    UIApplication *application = [UIApplication sharedApplication];

    BOOL enabled;

    // Try to use the newer isRegisteredForRemoteNotifications otherwise use the enabledRemoteNotificationTypes.
    if ([application respondsToSelector:@selector(isRegisteredForRemoteNotifications)])
    {
        enabled = [application isRegisteredForRemoteNotifications];
    }
    else
    {
        UIRemoteNotificationType types = [application enabledRemoteNotificationTypes];
        enabled = types & UIRemoteNotificationTypeAlert;
    }
    return enabled;
}

-(void) notifyRegistrationToAllWebViews {
    for(PluginReference* pluginReference in listOfPluginReferences){
        [pluginReference notifyRegistration];
    }
}

-(void) sendNotificationToAllWebViews {
    for(PluginReference* pluginReference in listOfPluginReferences){
        for(NSDictionary* notification in listOfNotifications){
            [pluginReference sendNotification:notification];
        }
    }
}

-(void) receivedNotification:(NSDictionary*)notification{
    NSLog(@"receivedNotification() -> %@", notification);

    [listOfNotifications addObject:notification];
    [self sendNotificationToAllWebViews];
}


-(void) didRegisterUserNotificationSettings:(UIUserNotificationSettings *)settings {
    if ([settings types] != UIUserNotificationTypeNone) {
        [[UIApplication sharedApplication] registerForRemoteNotifications];
    } else {
        [self notifyRegistrationToAllWebViews];
    }
}

-(void) onRegistered:(NSData *)deviceTokenData {

    self.deviceToken = [[[[deviceTokenData description] stringByReplacingOccurrencesOfString:@"<"withString:@""]
                        stringByReplacingOccurrencesOfString:@">" withString:@""]
                       stringByReplacingOccurrencesOfString: @" " withString: @""];
    
    [self notifyRegistrationToAllWebViews];
}

-(void) flushNotificationsToWebView:(PluginReference*)PluginReference {
    for(NSDictionary * notification in listOfNotifications){
        [PluginReference sendNotification:notification];
    }
}

-(void)removePluginInstance:(CDVPlugin*)plugin{
    PluginReference* pluginReference = [self findPluginReference:plugin];
    if(pluginReference){
        [pluginReference cleanUp];
        [listOfPluginReferences removeObject:pluginReference];
    }
}

-(void) addBackgroundCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin{
    PluginReference* pluginReference = [self getPluginReference:plugin];
    pluginReference.backgroundMessageCallBack = command;

    [self flushNotificationsToWebView:pluginReference];
}

-(void) addForegroundCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin{
    PluginReference* pluginReference = [self getPluginReference:plugin];
    pluginReference.foregroundMessageCallBack = command;

    [self flushNotificationsToWebView:pluginReference];
}

-(PluginReference*) createPluginReference:(CDVPlugin*)plugin {
    PluginReference* pluginReference = [PluginReference new];

    pluginReference.plugin = plugin;

    [listOfPluginReferences addObject:pluginReference];

    return pluginReference;
}

-(PluginReference*) findPluginReference:(CDVPlugin*)plugin {
    PluginReference* pluginReference = nil;
    for(PluginReference* item in listOfPluginReferences){
        if(item.plugin == plugin){
            pluginReference = item;
            break;
        }
    }
    return pluginReference;
}

-(PluginReference*) getPluginReference:(CDVPlugin*)plugin {
    PluginReference* pluginReference = [self findPluginReference:plugin];
    if (!pluginReference) {
        pluginReference = [self createPluginReference:plugin];
    }
    return pluginReference;
}

-(BOOL) isRegistrationComplete {
    return deviceToken != nil || failedToRegisterError != nil;
}

-(void) addRegistrationCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin{

    PluginReference* pluginReference = [self getPluginReference:plugin];
    pluginReference.registerCallBack = command;

    if ([self isRegistrationComplete]) {
        [pluginReference notifyRegistration];
    } else {
        NSDictionary* options = [command.arguments objectAtIndex:0];
        [self registerDevice:options];
    }
}


#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 80000
- (void) enableiOS8NotificationsWithBadgeEnabled:(BOOL) badgeEnabled SoundEnabled:(BOOL) soundEnabled AlertEnabled:(BOOL) alertEnabled {
    UIUserNotificationType notificationTypes = UIUserNotificationTypeNone;

    if (badgeEnabled)
        notificationTypes |= UIUserNotificationTypeBadge;

    if (soundEnabled)
        notificationTypes |= UIUserNotificationTypeSound;

    if (alertEnabled)
        notificationTypes |= UIUserNotificationTypeAlert;

    if (notificationTypes == UIUserNotificationTypeNone){
        NSLog(@"PushPlugin.register: Push notification type is set to none");
    }

    UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:notificationTypes categories:nil];

    [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
}
#endif

- (void) enableiOS7NotificationsWithBadgeEnabled:(BOOL) badgeEnabled SoundEnabled:(BOOL) soundEnabled AlertEnabled:(BOOL) alertEnabled {
    UIRemoteNotificationType notificationTypes = UIRemoteNotificationTypeNone;
    if (badgeEnabled)
        notificationTypes |= UIRemoteNotificationTypeBadge;

    if (soundEnabled)
        notificationTypes |= UIRemoteNotificationTypeSound;

    if (alertEnabled)
        notificationTypes |= UIRemoteNotificationTypeAlert;

    if (notificationTypes == UIRemoteNotificationTypeNone){
        NSLog(@"PushPlugin.register: Push notification type is set to none");
    }

    [[UIApplication sharedApplication] registerForRemoteNotificationTypes:notificationTypes];
}

-(BOOL)parseBool:(id)value {
    if(value == nil){
        return NO;
    }
    else if ([value isKindOfClass:[NSString class]]) {
        if ([value isEqualToString:@"true"]){
            return YES;
        }
    }
    else {
        return [value boolValue];
    }
    return NO;
}

-(void)registerDevice:(NSDictionary*) options{
    BOOL badgeEnabled = [self parseBool:[options objectForKey:@"badge"]];
    BOOL soundEnabled = [self parseBool:[options objectForKey:@"sound"]];
    BOOL alertEnabled = [self parseBool:[options objectForKey:@"alert"]];

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 80000
    if ([[UIApplication sharedApplication] respondsToSelector:@selector(registerUserNotificationSettings:)]) {
        // iOS8 in iOS8 SDK
        [self enableiOS8NotificationsWithBadgeEnabled:badgeEnabled SoundEnabled:soundEnabled AlertEnabled:alertEnabled];
    } else {
        // iOS7 in iOS8 SDK
        [self enableiOS7NotificationsWithBadgeEnabled:badgeEnabled SoundEnabled:soundEnabled AlertEnabled:alertEnabled];
    }
#else
    // iOS7 in iOS7 SDK
    [self enableiOS7NotificationsWithBadgeEnabled:badgeEnabled SoundEnabled:soundEnabled AlertEnabled:alertEnabled];
#endif
}

@end
