# Metric Insights Report

Date: 2026-05-24

This report outlines insight ideas for each dashboard metric in OpenVitals. It is intended as a product and implementation planning document, not as medical advice. Any future UI should frame these as wellness insights, trend summaries, and data interpretation helpers. The safest default is to compare users against their own baseline first, and only use public reference ranges where the source is strong and the wording is careful.

## Recommended Insight Pattern

Every metric detail view can share a common structure:

1. Overview visualization
   - Weekly: bar chart.
   - Monthly: calendar heatmap.
   - Yearly: dot heatmap.
   - This matches the hydration examples supplied by the user.

2. Statistic cards
   - Total.
   - Daily average.
   - Best day.
   - Days tracked.
   - Goals met.
   - Success rate.
   - Current streak.
   - Longest streak.
   - Change vs previous period.

3. Insight cards
   - Short natural-language summaries.
   - Examples: "Your average is 18% higher than last week" or "You met your goal 5 of 7 days."

4. Personal baseline
   - Compare the current period against a 30, 60, or 90 day baseline.
   - Prefer "above/below your usual range" over universal "good/bad" labels.

5. Data confidence
   - Show missing days.
   - Show source.
   - Show whether the value is measured, estimated, manually entered, or derived.

## Cross-Metric Foundation

These platform-level insight primitives would support nearly every metric:

- Period total.
- Period average.
- Daily average.
- Active/logged days.
- Missing days.
- Best day.
- Lowest day.
- Current streak.
- Longest streak.
- Goal completion count.
- Goal completion percentage.
- Change vs previous period.
- Change vs personal baseline.
- Weekday pattern.
- Source consistency.
- Estimated vs measured label.

## Hydration

Hydration is a strong first candidate for the full "History & Statistics" layout.

Recommended insights:

- Weekly, monthly, and yearly intake heatmap.
- Total intake for the selected period.
- Daily average.
- Best day.
- Current goal streak.
- Longest goal streak.
- Days tracked.
- Goals met.
- Success rate.
- Low-intake days.
- Intake consistency by weekday.
- Weekend vs weekday difference.
- Change vs previous week/month/year.
- Change vs personal baseline.

Example insight copy:

- "You reached your hydration goal 4 days in a row."
- "Your average this week is 22% lower than last week."
- "You usually drink less on weekends."
- "Your best intake day was Wednesday."

Reference and product note:

The National Academies adequate intake values are about 3.7 L/day for men and 2.7 L/day for women, but this includes water from foods and all beverages, not just logged water. The app should prefer a user-defined hydration goal rather than hardcoding a medical target.

## Steps

Recommended insights:

- Total steps.
- Daily average steps.
- Best step day.
- Current step-goal streak.
- Longest step-goal streak.
- Days above goal.
- Days below baseline.
- Sedentary/low-step days.
- Weekday pattern.
- Change vs previous period.
- Rolling 7-day average.
- Personal baseline comparison.

Example insight copy:

- "Your step average is trending upward over the last 4 weeks."
- "You walked 14% more than last week."
- "Your best step day was Tuesday."
- "You had 3 low-movement days."

Reference and product note:

Public health guidelines usually focus on minutes of moderate or vigorous activity rather than steps. Still, step volume is useful as a practical trend metric. A large meta-analysis found mortality benefits rose with more steps and plateaued roughly around 6,000-8,000 steps/day for adults 60+ and 8,000-10,000 steps/day for adults under 60.

## Distance

Recommended insights:

- Total distance.
- Daily average distance.
- Longest distance day.
- Distance goal streak.
- Distance per active day.
- Distance vs steps efficiency, if step data exists.
- Weekday pattern.
- Change vs previous period.
- Personal baseline comparison.

Example insight copy:

- "You covered 3.2 km more than last week."
- "Your longest walking day was Friday."
- "Distance increased while steps stayed stable, suggesting longer stride or faster movement."

## Total Calories

Recommended insights:

- Total calories burned.
- Daily average total calories.
- Highest burn day.
- Active day count.
- Total calories vs calories in, clearly labeled as an estimate when OpenVitals fills missing totals.
- Change vs previous period.
- Relationship with workouts.
- Relationship with steps/distance.

Example insight copy:

- "Total calories were 12% higher than last week."
- "Your highest burn day matched your longest workout."
- "Energy balance is estimated from logged intake and wearable output."

Product note:

Calorie burn from wearables is estimated. Use "estimated total calories" when OpenVitals derives missing totals from active calories plus BMR.

## Active Calories

Recommended insights:

- Total active calories.
- Daily average active calories.
- Best active day.
- Active calorie goal completion.
- Activity intensity distribution, if available.
- Change vs total calories burned.
- Active vs resting calorie proportion, if resting calories exist.

Example insight copy:

- "Active calories made up 28% of your total burn this week."
- "You reached your active calorie goal on 5 days."

## Floors Climbed

Recommended insights:

- Total floors.
- Daily average floors.
- Best climb day.
- Floors goal streak.
- Weekday pattern.
- Floors vs elevation consistency.
- Change vs previous period.

Example insight copy:

- "You climbed the most floors on Monday."
- "Floors climbed increased 18% compared with last week."

## Elevation

Recommended insights:

- Total elevation gained.
- Daily average elevation.
- Best elevation day.
- Elevation per distance.
- Hilly-day detection.
- Change vs previous period.
- Relationship with workout route/elevation, if available.

Example insight copy:

- "Your highest elevation day was Saturday."
- "Elevation per kilometer was higher than your usual range."

## Workout

Recommended insights:

- Total workout minutes.
- Number of sessions.
- Workout streak.
- Most common workout type.
- Longest workout.
- Average workout duration.
- Active minutes by intensity, if available.
- Weekly guideline progress.
- Strength-training days, if exercise type supports it.
- Recovery gaps.
- Change vs previous period.

Example insight copy:

- "You trained 4 days this week."
- "Your total workout time is 35 minutes above last week."
- "You have not logged strength training this week."
- "Your most consistent workout day is Saturday."

Reference and product note:

WHO and CDC guidance recommends adults get 150-300 minutes/week of moderate aerobic activity or 75-150 minutes/week of vigorous activity, plus muscle-strengthening activity on 2 or more days per week.

## Sleep

Recommended insights:

- Sleep duration trend.
- Sleep goal achievement.
- Sleep debt against user goal.
- Average bedtime.
- Average wake time.
- Sleep consistency score.
- Weekend vs weekday sleep shift.
- Longest sleep.
- Shortest sleep.
- Restless nights, if awake data exists.
- Stage distribution, if available.
- Change vs baseline.
- Relationship with HRV/resting heart rate.

Example insight copy:

- "You slept 48 minutes less than your 30-day average."
- "Your bedtime varied by 1h 20m this week."
- "You reached your sleep goal 5 of 7 nights."
- "HRV was higher after nights with 7+ hours of sleep."

Reference and product note:

CDC states adults generally need 7 or more hours of sleep per night. Avoid overinterpreting sleep stages because wearable sleep-stage accuracy varies.

## Calories In

Recommended insights:

- Total calories logged.
- Daily average calories.
- Highest-calorie day.
- Lowest-calorie logged day.
- Meal count.
- Logged days.
- Calorie goal completion.
- Calories in vs total calories, clearly labeled estimate.
- Change vs previous period.

Example insight copy:

- "You logged meals on 6 of 7 days."
- "Your calorie intake was 12% higher than last week."
- "Saturday was your highest logged intake day."

## Protein

Recommended insights:

- Total protein.
- Daily average protein.
- Protein per kg body weight, if weight exists.
- Days meeting protein target.
- Protein distribution by meal, if meal timestamps exist.
- Change vs previous period.
- Relationship with strength workouts.

Example insight copy:

- "Protein was below your target on 4 days."
- "Your average protein intake increased by 9 g/day."
- "Protein intake was highest on workout days."

Reference and product note:

The general adult protein RDA is often expressed as 0.8 g/kg/day, but needs vary by body size, age, training, and goals. Make protein goals customizable.

## Carbohydrates

Recommended insights:

- Total carbs.
- Daily average carbs.
- Carbs as percent of logged calories.
- Highest-carb day.
- Carb target completion.
- Change vs previous period.
- Carb distribution by meal, if available.
- Relationship with workout days.

Example insight copy:

- "Carbs made up 52% of logged calories."
- "Carb intake was higher on your longest workout day."

Reference and product note:

National Academies AMDR ranges for adults list carbohydrate at 45-65% of calories. This is a broad population range, not a personalized prescription.

## Fat

Recommended insights:

- Total fat.
- Daily average fat.
- Fat as percent of logged calories.
- Highest-fat day.
- Fat target completion.
- Change vs previous period.
- Macro balance with protein/carbs.

Example insight copy:

- "Fat made up 31% of logged calories."
- "Your fat intake was stable compared with last week."

Reference and product note:

National Academies AMDR ranges for adults list fat at 20-35% of calories. Prefer customizable user goals.

## Weight

Recommended insights:

- Latest weight.
- 7-day rolling average.
- 30-day trend.
- Change since previous period.
- Change since first logged value.
- Weekly rate of change.
- Lowest and highest in range.
- Weigh-in consistency.
- Relationship with hydration and cycle phase, if available.

Example insight copy:

- "Your 7-day average is stable."
- "Your weight changed by -0.4 kg over the last month."
- "You logged weight 5 times this month."

Product note:

Use rolling averages instead of focusing on single-day changes because daily weight fluctuates due to water, food, sodium, training, and menstrual cycle phase.

## Height

Recommended insights:

- Latest height.
- Source.
- Last update date.
- Height history, if multiple entries exist.
- BMI dependency note.

Example insight copy:

- "Height was last updated 8 months ago."
- "BMI is calculated using your latest height and weight."

Product note:

For adults, height usually changes slowly. This screen should be informational, not insight-heavy.

## BMI

Recommended insights:

- Current BMI.
- BMI category.
- BMI trend.
- Change vs previous period.
- Weight and height used for calculation.
- Optional goal progress, if the user enables weight goals.

Example insight copy:

- "BMI changed from 24.8 to 24.5 over 3 months."
- "BMI is calculated from your latest weight and height."

Reference and product note:

CDC adult BMI categories are: underweight below 18.5, healthy weight 18.5 to under 25, overweight 25 to under 30, and obesity 30 or higher. Include the caveat that BMI does not distinguish fat from muscle.

## Body Fat

Recommended insights:

- Latest body fat percentage.
- Trend over 30/90 days.
- Change since previous period.
- Estimated fat mass, if weight exists.
- Fat mass vs lean mass split.
- Source consistency.

Example insight copy:

- "Body fat is down 0.8 percentage points over 90 days."
- "Your latest reading came from the same source as your previous 5 readings."

Product note:

Consumer body-fat estimates can vary by device and hydration status. Use trend and source consistency instead of hard judgments.

## Lean Mass

Recommended insights:

- Latest lean mass.
- Trend.
- Change vs previous period.
- Lean mass as percentage of body weight.
- Relationship with protein and strength workouts.
- Source consistency.

Example insight copy:

- "Lean mass is stable while weight changed."
- "Lean mass readings are sparse this month."

## BMR

Recommended insights:

- Latest BMR.
- Trend.
- Relationship with weight and lean mass.
- Estimated maintenance calories if combined with activity, clearly labeled as estimated.
- Change vs previous period.

Example insight copy:

- "BMR changed because body weight or body composition changed."
- "BMR is an estimate, not a measured metabolic test."

## Bone Mass

Recommended insights:

- Latest bone mass.
- Trend.
- Source consistency.
- Missing or rare reading note.
- Change vs previous period.

Example insight copy:

- "Bone mass readings are expected to change slowly."
- "This is not the same as a clinical bone density scan."

Product note:

Do not imply osteoporosis screening. Consumer "bone mass" should be treated as a body composition trend estimate.

## Average Heart Rate

Recommended insights:

- Daily timeline.
- Minimum, average, and maximum.
- High-heart-rate periods.
- Previous-period comparison.
- Baseline comparison.
- Relationship with workouts.
- Relationship with sleep.

Example insight copy:

- "Average heart rate was 5 bpm higher than your baseline."
- "Highest average day was after your longest workout."

## Resting Heart Rate

Recommended insights:

- Morning/resting trend.
- 7-day average.
- Baseline deviation.
- Lowest resting heart rate.
- Recovery signal.
- Relationship with sleep and workouts.

Example insight copy:

- "Resting heart rate is 6 bpm above your 30-day baseline."
- "Resting heart rate decreased over the last 4 weeks."

Reference and product note:

NIH/NHLBI lists normal adult resting heart rate around 60-100 bpm, but trained athletes may be lower. Personal baseline is more useful than a universal "normal" label.

## HRV

Recommended insights:

- Rolling median HRV.
- Change vs 30-day baseline.
- Low-HRV streak.
- Recovery score candidate.
- Relationship with sleep.
- Relationship with workout load.
- Relationship with resting heart rate.

Example insight copy:

- "HRV was below your baseline 3 days in a row."
- "HRV improved on nights with 7+ hours of sleep."

Reference and product note:

HRV varies strongly by person, device, time, and measurement method. Avoid universal "good HRV" or "bad HRV" labels. Compare against the user's own baseline.

## Blood Pressure

Recommended insights:

- Latest systolic/diastolic.
- Period average.
- Category badge.
- Morning vs evening average.
- Reading count.
- Highest reading.
- Lowest reading.
- Change vs previous period.

Example insight copy:

- "Most readings this month were in the elevated range."
- "Your average systolic pressure is 6 mmHg lower than last month."

Reference and product note:

American Heart Association categories: normal is under 120/80, elevated systolic is 120-129 with diastolic under 80, stage 1 is 130-139 or 80-89, stage 2 is 140+ or 90+, and readings above 180 and/or 120 require urgent attention depending on symptoms. Present this carefully and encourage rechecking or medical care rather than diagnosing.

## SpO2

Recommended insights:

- Average oxygen saturation.
- Lowest reading.
- Time below threshold, if continuous data exists.
- Nighttime lows.
- Reading count.
- Source/device note.
- Baseline comparison.

Example insight copy:

- "Lowest SpO2 this week was 93%."
- "Most readings were within your usual range."

Reference and product note:

MedlinePlus says normal oxygen saturation is usually 95-100%, but pulse oximetry can differ from actual blood oxygen by several percentage points.

## VO2 Max

Recommended insights:

- Latest VO2 max.
- Trend over time.
- Change after training blocks.
- Cardio fitness category, if age/sex tables are available.
- Relationship with workout consistency.
- Change vs previous period.

Example insight copy:

- "VO2 max increased by 1.2 over 8 weeks."
- "Your best improvements followed weeks with 3 or more cardio sessions."

Reference and product note:

VO2 max is a strong cardiorespiratory fitness indicator, but wearables estimate it. Treat it as a trend unless lab-measured.

## Respiratory Rate

Recommended insights:

- Average respiratory rate.
- Overnight trend, if available.
- Baseline deviation.
- High/low outlier days.
- Relationship with sleep.
- Relationship with body temperature.

Example insight copy:

- "Respiratory rate is above your usual range this week."
- "Average respiratory rate stayed stable overnight."

Reference and product note:

MedlinePlus lists normal adult resting breathing around 12-18 breaths/min. Some sources use 12-20. Personal baseline is safer for consumer insight wording.

## Body Temperature

Recommended insights:

- Latest temperature.
- Baseline deviation.
- Overnight trend.
- Elevated-temperature flag, cautiously worded.
- Cycle-related temperature shifts.
- Change vs previous period.

Example insight copy:

- "Temperature is 0.4 C above your baseline."
- "Temperature returned to baseline after 2 days."

Reference and product note:

MedlinePlus says normal body temperature varies; one normal range is about 36.1-37.2 C. Time of day and measurement site matter.

## Mindfulness

Recommended insights:

- Total minutes.
- Session count.
- Average session length.
- Current streak.
- Longest streak.
- Days practiced.
- Best week.
- Relationship with sleep.
- Relationship with HRV.
- Relationship with resting heart rate.

Example insight copy:

- "You practiced mindfulness 4 days this week."
- "Sleep duration was higher on days with mindfulness sessions."
- "Your average session length is increasing."

Reference and product note:

NCCIH says mindfulness meditation may help reduce stress symptoms, anxiety/depression symptoms, and improve sleep, but evidence varies by practice and study quality.

## Cycle

Recommended insights:

- Current cycle day.
- Period days logged.
- Average cycle length.
- Cycle length variability.
- Average period length.
- Predicted next period, clearly marked as an estimate.
- Ovulation test count.
- Basal body temperature trend.
- Missed or late period detection, cautiously worded.
- Symptom trends, if symptoms are later added.

Example insight copy:

- "Your last 3 cycles averaged 29 days."
- "Cycle length varied by 6 days over the last 6 cycles."
- "BBT rose after the likely ovulation window."

Reference and product note:

Office on Women's Health says a normal adult menstrual cycle often ranges about 24-38 days. ACOG also frames the menstrual cycle as an important health sign. Avoid fertility guarantees.

## Highest-Value Implementation Order

1. Universal insight cards for all metric views
   - Total, average, best, tracked days, goal met, streak, previous-period change.

2. Hydration-style history views
   - Weekly bar, monthly calendar heatmap, yearly dot heatmap.

3. Goal engine
   - User-defined goals per metric.
   - Goals met.
   - Success rate.
   - Current and longest streaks.

4. Personal baseline engine
   - 30-day rolling baseline.
   - Current vs baseline.
   - Baseline deviation.
   - Anomaly detection.

5. Metric-specific interpretation
   - Blood pressure categories.
   - Sleep duration target.
   - BMI category.
   - Macro split.
   - Hydration goal.
   - Workout guideline progress.

6. Cross-metric insights
   - Sleep vs HRV.
   - Workouts vs resting heart rate.
   - Hydration vs weight fluctuation.
   - Mindfulness vs sleep.

## Implementation Notes

- Keep all insight calculations feature-owned or in a future shared `core/insights` package.
- Avoid moving metric-specific semantics into generic chart components.
- Persist goals separately from period selection.
- Every insight should be explainable from visible data.
- Use "may", "often", "estimated", and "usual range" where appropriate.
- Avoid diagnosis-like labels except where source-backed categories exist, such as BMI and blood pressure.
- Add source and data-quality context for Health Connect records whenever possible.

## Sources

- WHO physical activity guidelines: https://www.who.int/publications/i/item/9789240015128
- CDC adult physical activity guidance: https://www.cdc.gov/physical-activity-basics/guidelines/adults.html
- National Academies water intake reference: https://nap.nationalacademies.org/read/10925/chapter/6
- CDC adult sleep guidance: https://www.cdc.gov/sleep/about/index.html
- American Heart Association blood pressure categories: https://www.heart.org/en/health-topics/high-blood-pressure/understanding-blood-pressure-readings
- CDC adult BMI categories: https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html
- MedlinePlus pulse oximetry: https://medlineplus.gov/lab-tests/pulse-oximetry/
- MedlinePlus vital signs: https://medlineplus.gov/ency/article/002341.htm
- MedlinePlus body temperature norms: https://medlineplus.gov/ency/article/001982.htm
- Cleveland Clinic HRV overview: https://my.clevelandclinic.org/health/symptoms/21773-heart-rate-variability-hrv
- Harvard Health VO2 max overview: https://www.health.harvard.edu/staying-healthy/vo2-max-what-is-it-and-how-can-you-improve-it
- NCCIH meditation and mindfulness: https://www.nccih.nih.gov/health/meditation/overview.htm
- Office on Women's Health menstrual cycle: https://womenshealth.gov/menstrual-cycle/your-menstrual-cycle
- ACOG menstrual cycle as a vital sign: https://www.acog.org/clinical/clinical-guidance/committee-opinion/articles/2015/12/menstruation-in-girls-and-adolescents-using-the-menstrual-cycle-as-a-vital-sign
- National Academies macronutrient distribution ranges: https://www.nationalacademies.org/cdn/materials/9fb9fae6-337c-4b7c-9821-2c81d1f65ad0
- NIH Research Matters on daily steps: https://www.nih.gov/news-events/nih-research-matters/number-steps-day-more-important-step-intensity
- Daily steps and mortality meta-analysis: https://pmc.ncbi.nlm.nih.gov/articles/PMC9289978/
