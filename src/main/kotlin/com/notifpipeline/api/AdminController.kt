package com.notifpipeline.api

import com.notifpipeline.domain.model.DeliveryChannel
import com.notifpipeline.service.DlqReplayService
import com.notifpipeline.service.ReplayResult
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val dlqReplayService: DlqReplayService
) {

    @PostMapping("/dlq/replay")
    fun replayDlq(
        @RequestParam channel: String
    ): ResponseEntity<ReplayResult> {
        val deliveryChannel = try {
            DeliveryChannel.valueOf(channel.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val result = dlqReplayService.replay(deliveryChannel)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/dlq/replay/all")
    fun replayAllDlq(): ResponseEntity<List<ReplayResult>> {
        val results = DeliveryChannel.entries.map { channel ->
            dlqReplayService.replay(channel)
        }
        return ResponseEntity.ok(results)
    }
}