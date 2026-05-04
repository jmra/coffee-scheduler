package com.coffeescheduler.generator;

import com.coffeescheduler.model.Schedule;

public interface ScheduleGenerator {
    GeneratorResult generate(Schedule schedule);
}
