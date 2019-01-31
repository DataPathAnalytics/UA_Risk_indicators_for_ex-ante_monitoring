package com.datapath.persistence.entities.derivatives;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * SuppliersSingleBuyer
 * Постачальник виступає постачальником тільки для цього Замовника
 * Список постоянных поставщиков закупщика (>3 контрактов), которые работали только с 1 закупщиком
 * Формула: для каждого закупщика (procuringEntity scheme + id):
 * 1. Берем всех поставщиков (supplier scheme + id)
 * 2. Фильтруем среди них тех, у кого >3 контрактов
 * 3. Фильтруем среди оставшихся тех, у кого был ровно 1 закупщик
 * это и будут те, кто работал только с этим закупщиком
 * Список от момента расчета на 365 дней назад
 * Характеризирует закупщика
 */
@Entity
@Table(name = "suppliers_single_buyer")
@Data
public class SuppliersSingleBuyer {
    @Id
    @Column
    private Long id;

    @Column(name = "procuring_entity_id")
    private String buyerId;

    @Column(name = "supplier_id")
    private String supplier;

}
