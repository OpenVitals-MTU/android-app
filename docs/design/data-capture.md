<br />

## Introduction

![A community health worker looking at mobile device.](https://developers.google.com/static/open-health-stack/design/images/data-capture/intro.png)

Completing questionnaires is a core task for most healthcare workers using
mobile health apps.

Data entry can be difficult and errors happen. Our goal with the [Structured
Data Capture (SDC) library](https://developers.google.com/open-health-stack/android-fhir/data-capture) and the
design guidelines is to empower you to improve the user experience of data entry
and the quality of captured data.

The four themes covered in this section are:

1. [Layout \& navigation](https://developers.google.com/open-health-stack/design/data-capture-guideline#layout_navigation)
2. [Questions and instructions](https://developers.google.com/open-health-stack/design/data-capture-guideline#questions_instructions)
3. [Data capture](https://developers.google.com/open-health-stack/design/data-capture-guideline#data_capture)
4. [Data validation and error messages](https://developers.google.com/open-health-stack/design/data-capture-guideline#data_validation_and_errors)

## Layout \& navigation

![A stylized view of long scroll and paginated layouts.](https://developers.google.com/static/open-health-stack/design/images/data-capture/layoutbanner.png)

### Long scroll \& paginated layout

![Comparing long scroll and paginated layouts. Long scroll has 3
questions on one page, while paginated has one question.](https://developers.google.com/static/open-health-stack/design/images/data-capture/comparinglayouts.png) Long scroll layout (left) and paginated layout (right).

The Android FHIR SDK has two layout options for you to choose between:

1. Long scroll (default)
2. Paginated

A *long scroll* questionnaire shows all questions on one page and users navigate
to each question by scrolling.

A *paginated* questionnaire displays the content across separate pages. Related
questions or input fields can be grouped together on one page. Back and next
buttons are anchored at the bottom of the page to navigate between pages.

[Learn how to make a questionnaire paginated on
GitHub](https://github.com/ohs-foundation/android-fhir/wiki/SDCL%3A-Author-questionnaires#pagination)

### Which layout should you select?

Each layout option has its advantages and disadvantages. Below are some
attributes of each layout type to consider as you're making the choice about
which layout to use.

|   | Long scroll | Paginated |
| **Speed of navigation** | Faster to navigate | Slower to navigate |
| **Accuracy of navigation** | Less precise navigation | More precise navigation |
| **Refocus on question after task switching** | Difficult to reorient after interruption | Easier to reorient after interruption |
| **Completing the digital questionnaire after the visit (copying from paper)** | Easier when copying from paper | More difficult when copying from paper |
| **Small screens** | Worse for small screens | Better for small screens |
| **Accessibility** | Worse for accessibility. Difficult to navigate. | Better for accessibility. Discrete screens that can be handled by screen readers, text-to-speech, and other technologies. |
| **Space for instructions and explanations** | Worse for guidance and instructions | Better for guidance and instructions |
|---|---|---|

### Long scroll

![Questionnaire with numbers shown in front of question title.](https://developers.google.com/static/open-health-stack/design/images/data-capture/numberedquestionnaire.png) Yes  
Number the questions to make it easier to navigate in a single page layout. ![Comparing question title font size. Long scroll is 16px. Paginated is 28px.](https://developers.google.com/static/open-health-stack/design/images/data-capture/adjustfont.png) Yes  
Make the font size of question titles smaller when using long scroll, so more content is visible on the screen. Example: Long scroll is 16px. Paginated is 28px.

### Pagination

![Paginated question "What state do they live in?" with
dropdown selections.](https://developers.google.com/static/open-health-stack/design/images/data-capture/onequestion.png) Yes  
Keyboards, dropdowns and other components take up space on the page, so aim for one question per page. ![Paginated address fields where bottom field is not visible on screen.](https://developers.google.com/static/open-health-stack/design/images/data-capture/hidecontent.png) No  
Content should be visible above the fold. ![Multiple text fields grouped as one question. Question title is
alternative contact person, with name, relationship and phone number
as input fields.](https://developers.google.com/static/open-health-stack/design/images/data-capture/groupcontent.png) Yes  
Example: These three text fields are all related to alternative contact person info, so they are grouped together on one page. ![Unrelated questions on the same page. First question is insurance
coverage and the second question is prior health conditions.](https://developers.google.com/static/open-health-stack/design/images/data-capture/unrelatedgroup.png) No  
Avoid grouping unrelated content on one page, to avoid confusion.

### Progress indicator

The *progress indicator* reflects progress made within a questionnaire.

Include a *progress indicator* on long questionnaires to help users navigate and
see progress. *Progress indicators* show location within a questionnaire, and
how much is left to complete.
![Progress indicator located at top, below the questionnaire title.](https://developers.google.com/static/open-health-stack/design/images/data-capture/progress-scroll.png) Yes  
Position at top above the question and anchor so it is always visible even when scrolling. ![Progress indicator located at the bottom, above the navigation buttons.](https://developers.google.com/static/open-health-stack/design/images/data-capture/progress-paginated.png) Yes  
Can position at the bottom instead, above the back and next buttons. With this layout you can also display which page the user is on.

### Navigation Buttons

*Navigation buttons* (back, next) are anchored at the bottom of the
questionnaire. In an infinite scroll or on the last page of a paginated
questionnaire the next button is labeled Submit.

Keep buttons in a consistent location and always use active buttons that are
labeled with their action, such as back and next.
![Navigation buttons are active. Next button with blue fill.](https://developers.google.com/static/open-health-stack/design/images/data-capture/active-buttons.png) Yes  
Always display active buttons, even if forms are incomplete. Upon tapping next, show a pop-up dialog with instructions for completing missing fields or validation errors. ![Next button is inactive. Next button with gray fill.](https://developers.google.com/static/open-health-stack/design/images/data-capture/inactive-buttons.png) No  
Inactive buttons make it hard for users to know how to fix the problem. ![Next button only has an arrow icon and no text description.](https://developers.google.com/static/open-health-stack/design/images/data-capture/icon-buttons.png) No  
Avoid icon only buttons. Always label buttons with a descriptive action.

## Questions \& instructions

![Annotated paginated questionnaire showing questions and instructions components.](https://developers.google.com/static/open-health-stack/design/images/data-capture/question-banner.png) Overview of the 9 components covered in this section and how the components are combined in a paginated questionnaire.

1. Questionnaire title.
2. Progress indicator.
3. Group header.
4. Question title.
5. Instructions.
6. Input field.
7. Entry format.
8. Required fields.
9. Help.

### Group header

*Group header* is a text header that is displayed above question titles.

Use the *group header* to group similar questions together. Only use the *group
header* when it adds helpful information.
![Group header is Patient History.](https://developers.google.com/static/open-health-stack/design/images/data-capture/group-short.png) Yes  
Use a short title to group similar questions together. Example: all questions related to patient history are grouped. ![Group header is personal information and lifestyle information.](https://developers.google.com/static/open-health-stack/design/images/data-capture/group-long.png) No  
Avoid complex titles or long titles that go beyond one line.

### Question title

The *question title* succinctly describes what information is requested.
*Question titles* have the largest font size on the page to draw the user's eyes
to the question.

Every page or question should have a *question title*. Keep question titles
short or phrase it as a question.
![Question title is date of birth.](https://developers.google.com/static/open-health-stack/design/images/data-capture/title-short.png) Yes  
Short titles make it easier for users to read. ![Question title is What is your date of birth? Which city were you born in?](https://developers.google.com/static/open-health-stack/design/images/data-capture/title-long.png) No  
Avoid very long questions or nesting two questions together. ![No question title.](https://developers.google.com/static/open-health-stack/design/images/data-capture/title-none.png) No  
Always include a question title to make it easier for users to know what information they need to enter.

### Instructions

*Instructions* is an optional text field shown below the question title.

Use the *instructions* field to explain relevant instructions such as if the
question is required, how many selections can be made (one or many), and what
users should do if unable to complete all info or answer the question.
![Instructions: Select one. Required question.](https://developers.google.com/static/open-health-stack/design/images/data-capture/instructions-required.png) Yes  
Use instructions field to inform if a question is required and how many selections can be made. ![Instructions: If exact DOB is unknown select the unknown DOB checkbox.](https://developers.google.com/static/open-health-stack/design/images/data-capture/instructions-edge.png) Yes  
Use instructions to let users know what to do if they encounter a scenario like they are unable to complete all the fields. ![Instructions: The alternative contact would be used in the case of
an emergency situation and could be next of kin (e.g. partner,
mother, sibling.)](https://developers.google.com/static/open-health-stack/design/images/data-capture/instructions-definitions.png) Yes  
Use instructions to provide additional context or definitions for terms used in the question title.

### Label text

*Label text* informs users about what information is requested for a text field
or dropdown. When the field is selected, the *label text* moves from the middle
of the text field to the top.

Every *text field* and *dropdown* box should have a label. *Label text* should
be short, clear, and fully visible.
![Label text: First name.](https://developers.google.com/static/open-health-stack/design/images/data-capture/label-short.png) Yes  
Label text should be short, clear, and fully visible. ![Label text: enter the name of client.](https://developers.google.com/static/open-health-stack/design/images/data-capture/label-long.png) No  
Label text shouldn't be too long, truncated, or take up multiple lines. ![No label text.](https://developers.google.com/static/open-health-stack/design/images/data-capture/label-none.png) No  
Always label the text field so users know what information to enter.

### Entry format

*EntryFormat* is shown below the text field to inform users of the specific
format data needs to be entered in. Error messages will be displayed in the
EntryFormat field and replace existing EntryFormat instructions.

Use EntryFormat for dates, phone numbers, units, and integers.
![Date format: dd/mm/yyyy.](https://developers.google.com/static/open-health-stack/design/images/data-capture/entry-do.png) Yes  
Show date format below the field and include a descriptive phrase. ![No date format.](https://developers.google.com/static/open-health-stack/design/images/data-capture/entry-none.png) No  
Not showing data formats can lead to data being entered incorrectly. ![Below the Heart rate text field the Entry format shows: Normal
range: 60-100 bpm. Below the Blood oxygen saturation text field the
Entry format shows: Normal range: 95-100%.](https://developers.google.com/static/open-health-stack/design/images/data-capture/entry-range.png) Yes  
When entering medical ranges, provide examples of the normal range. This can help users catch errors or numbers that are out of range.

### Required fields

*Required fields* indicate that a user must complete the field and is blocked
from advancing until the field is completed.

To indicate that a field is required, display an asterisk (\*) at the end of the
question title. Include 'required question' in the instructions field as it is
not obvious to everyone what an asterisk (\*) indicates. If there's no question
title, display the asterisk (\*) in the label text.
![Asterisk after question title and required question in instructions below.](https://developers.google.com/static/open-health-stack/design/images/data-capture/required-explanation.png) Yes  
Show the field is required with asterisk (\*) and include written instructions that indicate \`required question.\` Many are unfamiliar with what the asterisk(\*) means and would benefit from the explanation. ![Asterisk after question title, but no written description explaining what asterisk means.](https://developers.google.com/static/open-health-stack/design/images/data-capture/required-noexplanation.png) No  
Avoid showing only the asterisk (\*) without any written description of what it means. ![Asterisk after question title and mandatory question is shown in instructions below.](https://developers.google.com/static/open-health-stack/design/images/data-capture/required-localize.png) Yes  
Use the terms that are most familiar to your users. Example: "Mandatory" might be the more familiar term and used in some countries instead of "Required". ![No asterisk. Optional question is shown in the instructions.](https://developers.google.com/static/open-health-stack/design/images/data-capture/required-optional.png) Yes  
If most questions are required, indicate which ones are optional instead. ![Asterisk after label text. Required question shown in Entry format field.](https://developers.google.com/static/open-health-stack/design/images/data-capture/required-label.png) Yes  
If there's no question title show the asterisk in the label text.

### Help

A *help* icon is displayed next to the question title. Upon tapping the icon, a
help information box appears with additional information. Tapping the icon again
closes the help information box.

This is an optional component. Only use when helpful to display additional
information that does not need to always be visible.
![Help: The seasonal flu vaccine is also called influenza immunization.](https://developers.google.com/static/open-health-stack/design/images/data-capture/help-do.png) Yes  
Use help for information that users might only need to see once or that provides additional information. ![Help: Select one.](https://developers.google.com/static/open-health-stack/design/images/data-capture/help-dont.png) No  
Avoid hiding instructions inside the Help box that should be visible to everyone.

## Data capture

![Eight data capture components; text fields, date picker, dropdown,
slider, single choice, boolean choice, multiple choice and open
choice.](https://developers.google.com/static/open-health-stack/design/images/data-capture/datacapture-banner.png) Eight of the primary data capture components in the Android FHIR SDK.

### When to use which component?

| Type of data entry | Boolean choice | Single choice | Multiple choice | Open choice | Dropdown | Date picker | Text field | Slider | Auto-complete |
| Select Yes or No |   |   |   |   |   |   |   |   |   |
| Select one option |   |   |   |   |   |   |   |   | caution |
| Select multiple options |   |   |   |   |   |   |   |   | caution |
| Text |   |   |   |   |   |   |   |   |   |
| Dates |   |   |   |   |   |   |   |   |   |
| Numbers |   |   |   |   |   |   |   | caution |   |
|---|---|---|---|---|---|---|---|---|---|

### Text fields

*Text fields* indicate that users can enter information.

Use *text fields* when someone needs to enter text into the questionnaire, such
as a name, phone number, or address. Limit data entry that requires text
(keyboard) entry when a pre-populated selection (multiple choice or single
choice) can be used instead.

[Learn more about text fields on
material.io](https://m3.material.io/components/text-fields/overview)
![Question title: register a new person. Text field 1: name. Text
field 2: phone number.](https://developers.google.com/static/open-health-stack/design/images/data-capture/text-do.png) Yes  
Use text fields for data entry that requires typing unique words or numbers. ![Question title: reason for visit? Text field: describe reason](https://developers.google.com/static/open-health-stack/design/images/data-capture/text-dont.png) No  
Avoid using free text responses when it could be a multiple-selection, dropdown, or single choice selection instead.

### Single choice \& boolean choice

*Single choice* and *boolean choice* are a selection control that appear as
radio buttons when users are asked to select one choice from options.

Use *boolean choice* when there's a binary choice of 'Yes' or 'No.' Otherwise,
use the *single choice* component. If there's more than \~10 options in the list,
use a *dropdown* instead of *single choice*. A dropdown is more dense and easier
to navigate when there are many options.
![Question title: is this their first visit? Boolean selection options
are yes and no.](https://developers.google.com/static/open-health-stack/design/images/data-capture/singlechoice-boolean.png) Yes  
Use Boolean choice when the options are 'yes' and 'no'. ![Question title: What is the highest level of education achieved?
Single choice options are: 1. Does not know 2. No education

3. primary school 4. secondary school.](https://developers.google.com/static/open-health-stack/design/images/data-capture/singlechoice-single.png) Yes  
Use single choice when users can select one option in the list. ![Single choice list showing a very long list of states. States 23-27
are visible.](https://developers.google.com/static/open-health-stack/design/images/data-capture/singlechoice-dont.png) No  
Avoid single choice for very long lists (10+). Use a dropdown instead.

### Date picker

The *date picker* allows users to enter dates through both the calendar date
picker and the keyboard. The calendar date picker is activated when the calendar
icon is tapped.

Use the calendar date picker only for dates that are close to today's date such
as last menstrual period or next visit. Otherwise prioritize keyboard entry for
dates like birthdate.
![Date of birth. Keyboard date entry is active. Calendar icon on right
side of text field box. Checkbox is checked indicating that date is
approximate.](https://developers.google.com/static/open-health-stack/design/images/data-capture/datepicker-do.png) Yes  
For entering dates enable both keyboard entry (tapping text box) and calendar date picker (tapping icon). ![Calendar view of date picker.](https://developers.google.com/static/open-health-stack/design/images/data-capture/datepicker-dont.png) No  
Avoid enabling the calendar date picker as the only input method for birth dates. Navigating to the month and year is difficult.

### Dropdown

*Dropdown* menus allow users to make a selection from multiple options. As the
user begins typing, options filter based on what's entered. This can help users
quickly find the right option from a large list.

*Dropdown* menus are a great alternative to *single choice* when the list of
options is very long (10+ options) as they take up less space.
![Dropdown for State listing States A-F.](https://developers.google.com/static/open-health-stack/design/images/data-capture/dropdown-do.png) Yes  
Use a dropdown when selecting one option in a very long list of options, such as selecting a state or city. ![Dropdown for age, listing numbers one through six.](https://developers.google.com/static/open-health-stack/design/images/data-capture/dropdown-dont.png) No  
Avoid using a dropdown when it would be easier to type the content in rather than scrolling through all the options, such as age.

### Multiple choice

*Multiple choice* is a selection control that appears as checkboxes when users
can make multiple sections from a list of options.

Use *multiple choice* when users can only select from a predetermined list of
options. If users can also add their own free response, use the *open choice*
component instead. In the *instructions* field write "Select all that apply" so
users know they can select multiple options.
![Question title: What's the reason for today's visit? Four checkboxes
and options shown, one on each row.](https://developers.google.com/static/open-health-stack/design/images/data-capture/multiple-do.png) Yes  
The default appearance is a container around checkboxes to make the tappable area obvious. ![Question title: what's the reason for today's visit. Six checkboxes
and options shown, two on each row. Some text is cut off for two of
the options.](https://developers.google.com/static/open-health-stack/design/images/data-capture/multiple-dont.png) No  
Avoid displaying multiple options per row, due to the variation in phone screen size and text size, the text can get cut off.

### Open choice

*Open choice* is similar to multiple choice, but adds the ability for a user to
select
**Other** and type in free text.

Use *open choice* when there's a pre-set list of options, but users can also add
additional options. Use *open choice* when the majority of options are known,
but you foresee some users will select **Other** because none of the supplied
options apply.
![Other is selected. Text field for adding free text is active.
Keyboard is visible.](https://developers.google.com/static/open-health-stack/design/images/data-capture/openchoice-do.png) Yes  
Use when it's important that accurate data is collected and none of the predefined options apply. Example: occupation. ![Question title: Anything else to add? Three options: Yes, No, and
Other. Other is selected. Text field for adding free text is active.](https://developers.google.com/static/open-health-stack/design/images/data-capture/openchoice-dont.png) No  
Avoid using if the majority of responses would require selecting **Other**. In that case, use a text field or paragraph field instead.

### Slider

*Sliders* allow users to make selections from a range of values. The slider in
the Android FHIR SDK is a discrete slider. A discrete slider allows users to
select a specific value from a predetermined range. Tick marks may be used to
indicate available values. Avoid using the slider for numerical data entry.
Instead use a text field or a dropdown menu.

[Learn more about sliders on
material.io](https://m3.material.io/components/sliders/guidelines)
![Question title: how many children does the client have? A slider
with the number four is selected.](https://developers.google.com/static/open-health-stack/design/images/data-capture/slider-dont.png) No  
Avoid using the slider for specific values when there's a large range. Use text fields with keyboard entry instead.

## Data validation and errors

![Decorative examples of data validation.](https://developers.google.com/static/open-health-stack/design/images/data-capture/validationerror-banner.png)

### Data validation

Data validations constrain the type of data or the values that can be entered in
a text field. Data validation can improve the quality of data collected.

Use the *EntryFormat* field to display format or value restrictions. Show
meaningful data validation error messages in-line and immediately so users can
fix the error.
![Label text: phone number. Entry format: 8 digits.](https://developers.google.com/static/open-health-stack/design/images/data-capture/validation-show.png) Yes  
Show data validation restrictions upfront so users know how to enter the data. ![Label text: phone number. Entry format: none.](https://developers.google.com/static/open-health-stack/design/images/data-capture/validation-hide.png) No  
Without showing how many digits the phone number needs to be, users are likely to encounter an error and it will take longer to complete. ![Date entered is 22/33/4444. Error message: Wrong date format. Format
must be: dd/mm/yyyy.](https://developers.google.com/static/open-health-stack/design/images/data-capture/validation-timely.png) Yes  
Show meaningful data validation errors immediately after completing the field. Error messages replace the existing entry format text. ![Dialog. Fix the following errors. 1. phone number. 2. date of birth.
Button 1: submit anyway. Button 2: fix errors.](https://developers.google.com/static/open-health-stack/design/images/data-capture/validation-dontwait.png) No  
Don't wait until the user has pressed "submit" to display validation errors for the first time.

### Errors

Error messages alert users when something goes wrong and communicate how to fix
the problem.

Use color, iconography and text to communicate errors.

[Learn more about error messages on
material.io](https://m3.material.io/components/text-fields/guidelines#9ad90554-a793-4506-9075-6812fd7b381a)
![Error message is 'Required question. Select one.'](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-howtofix.png) Yes  
Explain why there's an error (required question) and what can be done to fix it (select one.) ![Error message is 'Error'.](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-noexplanation.png) No  
An error message that only says "error" is not helpful for users to know how to fix the error. ![Date of birth. Date entered is 22/33/4444. Error message is Wrong
date format. Format must be: dd/mm/yyyy.](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-noblame.png) Yes  
Example: "Wrong date format. Format must be dd/mm/yyyy". ![Date of birth. Date entered is 22/33/4444. Error message is 'You
entered the wrong date format.'](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-dontblame.png) No  
Avoid blaming the user with error messages that include "you" Example: "You entered the wrong date format." ![Error message with an icon in front of red text displaying 'Required
question. Select one or more options.' Checkbox containers have a
red outline.](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-combo.png) Yes  
Use color, iconography and text to inform users that there is an error. ![No error message or icon. Checkbox containers have a red outline,
which is the only indicator that there's an error.](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-onlycolor.png) No  
To support common visual impairments such as red-green color blindness, avoid relying only on color to communicate an error. ![Checkbox containers have a red outline and one error icon displayed
behind each container. 3 icons are visible.](https://developers.google.com/static/open-health-stack/design/images/data-capture/error-icons.png) No  
One icon is often enough. Don't overdo it on the use of icons to communicate the error.