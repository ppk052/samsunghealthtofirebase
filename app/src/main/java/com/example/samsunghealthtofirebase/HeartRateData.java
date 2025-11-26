package com.example.samsunghealthtofirebase;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeartRateData {
    private LocalDateTime HeartRateDate;
    private long HeartRate;
}
