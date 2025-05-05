package com.auto.trader.position.entity;

import com.auto.trader.position.enums.IndicatorType;
import com.auto.trader.position.enums.Operator;
import com.auto.trader.position.enums.Direction;
import com.auto.trader.position.enums.Timeframe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private IndicatorType type;

    private Double value;
    private Double k;
    private Double d;

    @Enumerated(EnumType.STRING)
    private Operator operator;

    @Enumerated(EnumType.STRING)
    private Timeframe timeframe;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;
}
