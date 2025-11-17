package com.agv.AGVBackend;
import lombok.Setter;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Setter
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agvId;
    private int x;
    private int y;
    private String status;

    private LocalDateTime timestamp = LocalDateTime.now();

    public Telemetry() {}
}
