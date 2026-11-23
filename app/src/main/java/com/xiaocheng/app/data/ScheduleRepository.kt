package com.xiaocheng.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduleRepository(private val prefs: Prefs) {

    private val _events = MutableStateFlow(prefs.loadEvents())
    val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

    private fun commit(list: List<ScheduleEvent>) {
        _events.value = list
        prefs.saveEvents(list)
    }

    fun add(event: ScheduleEvent) = commit(_events.value + event)

    fun addAll(events: List<ScheduleEvent>) = commit(_events.value + events)

    fun update(event: ScheduleEvent) {
        event.updatedAt = System.currentTimeMillis()
        commit(_events.value.map { if (it.id == event.id) event else it })
    }

    fun delete(id: String) = commit(_events.value.filterNot { it.id == id })

    fun deleteMany(ids: Set<String>) = commit(_events.value.filterNot { it.id in ids })

    fun markDone(ids: Set<String>) {
        val now = System.currentTimeMillis()
        commit(_events.value.map {
            if (it.id in ids) it.copy(done = true, updatedAt = now) else it
        })
    }

    fun markUndone(ids: Set<String>) {
        val now = System.currentTimeMillis()
        commit(_events.value.map {
            if (it.id in ids) it.copy(done = false, updatedAt = now) else it
        })
    }

    fun toggleDone(id: String) {
        commit(_events.value.map {
            if (it.id == id) it.copy(done = !it.done, updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun clear() = commit(emptyList())

    fun seedExamplesIfNeeded() {
        if (prefs.seeded || _events.value.isNotEmpty()) return
        prefs.seeded = true
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        fun at(dayOffset: Long, hour: Int, minute: Int = 0): Long =
            now.toLocalDate().plusDays(dayOffset).atTime(LocalTime.of(hour, minute))
                .atZone(zone).toInstant().toEpochMilli()

        val examples = listOf(
            ScheduleEvent(
                title = "校园程序设计大赛 · 报名截止",
                deadlineAt = at(3, 23, 59),
                location = "线上提交",
                category = "竞赛",
                note = "示例日程：可点开编辑，或在「个人 - 数据管理」里清空。",
                source = "示例",
            ),
            ScheduleEvent(
                title = "AI 前沿讲座 · 示例",
                startAt = at(1, 19, 0),
                location = "图书馆报告厅",
                category = "讲座",
                note = "示例日程：主讲人将从大模型的信息抽取能力讲起。",
                source = "示例",
            ),
            ScheduleEvent(
                title = "社团招新面试",
                startAt = at(5, 14, 0),
                location = "大学生活动中心 302",
                category = "活动",
                note = "示例日程：记得带上报名表。",
                source = "示例",
            ),
            ScheduleEvent(
                title = "体测预约确认",
                deadlineAt = at(-2, 18, 0),
                category = "考试",
                note = "示例日程：已经过期，用来展示「已到期」分组。",
                source = "示例",
                done = false,
            ),
        )
        commit(examples)
    }
}