//
//  NotificationService.h
//  AppGyver
//
//  Created by Rafael Almeida on 19/12/14.
//  Copyright (c) 2014 AppGyver Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "CDV.h"
#import "CDVPlugin.h"

@interface NotificationService : NSObject

@property (strong, nonatomic) NSString* deviceToken;

@property (strong, nonatomic) NSString* failedToRegisterError;

@property (strong, nonatomic) NSMutableArray* listOfPluginReferences;

@property (strong, nonatomic) NSMutableArray* listOfNotifications;


+(NotificationService*) instance;

-(void) removePluginInstance:(CDVPlugin*)plugin;

-(void) receivedNotification:(NSDictionary*)notification;

-(void)failToRegister:(NSError*)error;

-(void) unRegister;

-(bool) notificationsEnabled;

-(void) addBackgroundCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin;

-(void) addForegroundCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin;

-(void) addRegistrationCallBack:(CDVInvokedUrlCommand*)command plugin:(CDVPlugin*) plugin;

-(void) didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings;

-(void) onRegistered:(NSData *)deviceToken;

@end
