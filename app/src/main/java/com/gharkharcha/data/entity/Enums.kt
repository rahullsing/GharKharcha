package com.gharkharcha.data.entity

/** Room stores enums by [Enum.name]; never rename constants without a migration. */

enum class WorkType { ELECTRICAL, PLUMBING, BRICK_MASONRY, PAINTING, TILING, CARPENTRY, CUSTOM }

enum class WorkStatus { PLANNED, IN_PROGRESS, DONE, ON_HOLD }

enum class MaterialUnit { METRE, FEET, KG, BAG, PIECE, BOX, LITRE, SQ_FT, CUSTOM }

/** How a material purchase was paid. */
enum class MaterialPaymentMode { CASH, UPI, CARD, BANK, CREDIT }

enum class Skill { ELECTRICIAN, BRICK_MASON, HELPER, PLUMBER, CARPENTER, PAINTER, TILE_FITTER, WELDER, CUSTOM }

enum class AttendanceStatus { FULL_DAY, HALF_DAY, HOURS, ABSENT, OVERTIME }

enum class RateType { DAILY, HOURLY }

/** How a labourer was paid. */
enum class PaymentMode { CASH, UPI, BANK, CHEQUE }

/** WAGE settles earned pay; ADVANCE is money given ahead of work. Both reduce the balance due. */
enum class PaymentType { WAGE, ADVANCE }

enum class OwnerType { PROJECT, WORK, MATERIAL, LABOURER, PAYMENT }

enum class AttachmentKind { PHOTO, BILL, RECEIPT, PROFILE, COVER }
