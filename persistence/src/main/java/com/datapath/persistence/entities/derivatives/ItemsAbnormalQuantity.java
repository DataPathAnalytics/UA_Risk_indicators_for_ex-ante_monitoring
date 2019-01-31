package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/** ItemsAbnormalQuantity
   CPV2	- Значение 0.998-квантиля по количеству айтемов в CPV2
   для каждого cpv2: quantile(length(data.items) group by relatedLot, 0.998)
 */
@Entity
@Table(name = "items_abnormal_quantity")
@Data
public class ItemsAbnormalQuantity {
    @Id
    @Column
    private Long id;

    @Column(name = "cpv2", length = 8)
    private String cpv;

    @Column(name = "percentile_count")
    private Double percentileCount;

}
