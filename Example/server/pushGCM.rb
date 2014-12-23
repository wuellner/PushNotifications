require 'rubygems'
require 'pushmeup'

APPLICATION_API_KEY = "API_KEY_GOES_HERE"
DEVICE_REGISTRATION_ID = "REGISTRATION_ID_GOES_HERE"

GCM.host = 'https://android.googleapis.com/gcm/send'
GCM.format = :json
GCM.key = APPLICATION_API_KEY
destination = [DEVICE_REGISTRATION_ID]
data = {:message => "PhoneGap Build rocks!", :msgcnt => "1", :soundname => "beep.wav"}

puts GCM.send_notification( destination, data)
