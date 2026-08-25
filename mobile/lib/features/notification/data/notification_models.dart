class NotificationModel {
  NotificationModel({
    required this.id,
    required this.userId,
    required this.title,
    required this.message,
    required this.type,
    required this.read,
    required this.createdAt,
  });

  final String id;
  final String userId;
  final String title;
  final String message;
  final String type;
  final bool read;
  final String createdAt;

  factory NotificationModel.fromJson(Map<String, dynamic> json) => NotificationModel(
    id: json['id'] as String,
    userId: json['userId'] as String,
    title: json['title'] as String,
    message: json['message'] as String,
    type: json['type'] as String? ?? 'SYSTEM',
    read: json['read'] as bool? ?? false,
    createdAt: json['createdAt'] as String? ?? '',
  );
}
