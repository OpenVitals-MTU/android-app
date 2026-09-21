package tech.mmarca.openvitals.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderSettings
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderSettings

/** The screens' view of the reminder controllers, which need a Context of their own. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemindersModule {

    @Binds
    abstract fun bindHydrationReminderSettings(impl: HydrationReminderController): HydrationReminderSettings

    @Binds
    abstract fun bindMindfulnessReminderSettings(
        impl: MindfulnessReminderController,
    ): MindfulnessReminderSettings
}
