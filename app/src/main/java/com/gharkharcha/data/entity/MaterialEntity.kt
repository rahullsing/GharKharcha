package com.gharkharcha.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A material purchase under a Work. Item details are optional so a "total-only"
 * entry (e.g. "Raw material – ₹4,500") is just [itemName] + [totalPaise].
 *
 * [quantityMilli] is quantity × 1000 (2.5 bags = 2500) to avoid floating point.
 */
@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VendorEntity::class,
            parentColumns = ["id"],
            childColumns = ["vendor_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("work_id"), Index("vendor_id"), Index("purchase_date")],
)
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "work_id") val workId: Long,
    @ColumnInfo(name = "item_name") val itemName: String,
    val brand: String = "",
    @ColumnInfo(name = "quantity_milli") val quantityMilli: Long? = null,
    val unit: MaterialUnit? = null,
    @ColumnInfo(name = "custom_unit") val customUnit: String = "",
    @ColumnInfo(name = "rate_paise") val ratePaise: Long? = null,
    @ColumnInfo(name = "total_paise") val totalPaise: Long,
    /** True when the user typed a total instead of using qty × rate. */
    @ColumnInfo(name = "total_overridden") val totalOverridden: Boolean = false,
    @ColumnInfo(name = "vendor_id") val vendorId: Long? = null,
    @ColumnInfo(name = "shop_name") val shopName: String = "",
    @ColumnInfo(name = "shop_phone") val shopPhone: String = "",
    @ColumnInfo(name = "purchase_date") val purchaseDate: LocalDate,
    @ColumnInfo(name = "bill_number") val billNumber: String = "",
    @ColumnInfo(name = "payment_mode") val paymentMode: MaterialPaymentMode = MaterialPaymentMode.CASH,
    @ColumnInfo(name = "is_paid") val isPaid: Boolean = true,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
