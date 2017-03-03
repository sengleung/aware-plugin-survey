AWARE Survey Plugin
===================

A time and application-usage triggered mobile research survey plugin for the AWARE Framework.

Deployment
==========

The following instructions are intended for the end-user/surveyee.

## Installation

1. Ensure that the [AWARE application](https://play.google.com/store/apps/details?id=com.aware.phone&hl=en) is installed on the device.
2.  Enable permissions for the application.

		Settings -> Accessibility -> AWARE -> on		

3. Transfer the **com.aware.plugin.survey.apk** file to the device from the directory:

		\apk
	
4. Install the **com.aware.plugin.survey.apk** onto the device.
5. Accept any permissions requests from the plugin.

## Software Dependencies

* The minimum Android version is 4.4.1+.
* This plugin must be used with the [AWARE](https://play.google.com/store/apps/details?id=com.aware.phone&hl=en) base application.

## Demonstration

![](https://github.com/sengleung/aware-plugin-survey/blob/master/assets/aware-plugin-survey-demo.gif)

Configuration
=============

The following instructions are intended for the developer/surveyor.

## Triggers

The **esm.ini** file is the configuration file for the **triggers** of *Experience Sampling Method (ESM)* questionnaires.

The **esm.ini** file is located in:

	com.aware.plugin.survey\src\main\res\raw\

The file is a plain-text file.
An example **esm.ini** file is provided below:

	[ESM_1_SPECIFIC_TIMES]
	Trigger=time
	ESM=esm1
	Times=12:00,13:00,14:00,21:10,13:12
	
	[ESM_2_APPLICATION_OPEN_CLOSE]
	Trigger=app-open-close
	ESM=esm2
	Applications=Chrome,Facebook
	Open=true
	Close=false

* The `[ESM_1_SPECIFIC_TIMES]` represents the title of the questionnaire. The title is not parsed and so it can be modified by the user. A line beginning with `[` or new lines are ignored.

* `Trigger=time` represents the type of the trigger. The name of the trigger `time` is constant and is predefined. Parameters that follow are constant and is is specific to each trigger.

* `ESM=esm1` is the questionnaire JSON file name which the trigger corresponds to. Note that the file extension is not included.

* `Times=12:00,13:00,14:00,21:10,13:12` is the parameter unique to the `time` trigger. Multiple values are allowed with a comma between the values. Note the absence of space between the times.

* The **esm.ini** file can contain multiple triggers. The file name of the **.ini** file must not be changed.


### Specific Times

Questionnaire is triggered at specific times.

	[ESM_1_SPECIFIC_TIMES]
	Trigger=time
	ESM=esm1
	Times=12:01,13:14,14:34,21:32,13:09

* `Times`

  The time of the day in which the questionnaire is triggered.   Represented in `HH:MM` format. Multiple times can be set for the same questionnaire.

### Application Open/Close

Questionnaire is triggered at application opening and/or closing.

	[ESM_2_APPLICATION_OPEN_CLOSE]
	Trigger=app-open-close
	ESM=esm2
	Applications=Chrome,Facebook
	Open=true
	Close=false

* `Applications`

  A single application name or a list of application names which displays a questionnaire when opening and/or closing.

* `Open`

  Display questionnaire when application opens. Only accepts `true` or `false`.
  
* `Close`

  Display questionnaire when application closes. Only accepts `true` or `false`.

## Questionnaires

The **&lt;file_name>.json** file contains a single question or a list of questions in sequence for the *Experience Sampling Method (ESM)* questionnaires. Each file corresponds to the respective trigger options from the **esm.ini** file.

The **&lt;file_name>.json** file is located in:

	com.aware.plugin.survey\src\main\res\raw\

The file is a plain-text file.
An example **esm1.ini** file is provided below:

	[
		{"esm": {
			"esm_type":1,
			"esm_title":"ESM Freetext",
			"esm_instructions":"The user can answer an open ended question.",
			"esm_submit":"Next",
			"esm_expiration_threshold":60,
			"esm_trigger":"AWARE Tester"
		}},
	
		{"esm": {
			"esm_type":2,
			"esm_title":"ESM Radio",
			"esm_instructions":"The user can only choose one option",
			"esm_radios":["Option one","Option two","Other"],
			"esm_submit":"Next",
			"esm_expiration_threshold":60,
			"esm_trigger":"AWARE Tester"
		}},
	]

 * Questions together can be chained in sequence.
 * The JSON question parameters are determined the AWARE framework.
 * The filename of the **.json** file can be changed, but it must  correspond to the parameter name in the **esm.ini** file.
 * The JSON templates below provide the different types of questionnaires.
 * Further information can be found at http://www.awareframework.com/esm/.
 
### Free Text

	{"esm": {
		"esm_type":1,
		"esm_title":"ESM Freetext",
		"esm_instructions":"The user can answer an open ended question.",
		"esm_submit":"Next",
		"esm_expiration_threshold":60,
		"esm_trigger":"AWARE Tester"
	}}

### Radio Button

	{"esm": {
		"esm_type":2,
		"esm_title":"ESM Radio",
		"esm_instructions":"The user can only choose one option",
		"esm_radios":["Option one","Option two","Other"],
		"esm_submit":"Next",
		"esm_expiration_threshold":60,
		"esm_trigger":"AWARE Tester"
	}}
	
### Checkbox

	{"esm": {
		"esm_type":3,
		"esm_title":"Checkbox",
		"esm_checkboxes":["Option 1","Option 2","Other"],
		"esm_submit":"OK",
		"esm_instructions":"Multiple choice is allowed"
	}}
	
### Likert Scale

	{"esm": {
		"esm_type":4,
		"esm_title":"Likert",
		"esm_likert_max":5,
		"esm_likert_max_label":"Great",
		"esm_likert_min_label":"Poor",
		"esm_likert_step":1,
		"esm_instructions":"Likert ESM",
		"esm_submit":"OK"
	}}
	
### Quick Answer

	{"esm": {
		"esm_type":5,
		"esm_title":"ESM Quick Answer",
		"esm_instructions":"One touch answer",
		"esm_quick_answers":["Yes","No","Maybe"],
		"esm_expiration_threshold":60,
		"esm_trigger":"AWARE Tester"
	}}
	
###Scale

	{"esm": {
		"esm_type":6,
		"esm_title":"ESM Scale",
		"esm_instructions":"Between 0 and 10 with 2 increments",
		"esm_scale_min":0,
		"esm_scale_max":10,
		"esm_scale_start":0,
		"esm_scale_max_label":"10",
		"esm_scale_min_label":"0",
		"esm_scale_step":2,
		"esm_submit":"OK",
		"esm_expiration_threshold":60,
		"esm_trigger":"AWARE Tester"
	}}
	
### Numeric

	{"esm": {
		"esm_type":7,
		"esm_title":"ESM Numeric",
		"esm_instructions":"The user can enter a number.",
		"esm_submit":"Next",
		"esm_expiration_threshold":60,
		"esm_trigger":"AWARE Tester"
	}}
