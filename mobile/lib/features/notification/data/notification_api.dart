import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/notification/data/notification_models.dart';

class NotificationApi {
  NotificationApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<List<NotificationModel>> listNotifications({int page = 0, int size = 20}) async {
    final json = await _client.get('/notifications', query: {
      'page': page.toString(),
      'size': size.toString(),
    });
    final content = (json as Map<String, dynamic>)['content'] as List<dynamic>;
    return content.map((e) => NotificationModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<int> getUnreadCount() async {
    final json = await _client.get('/notifications/unread-count');
    return (json as Map<String, dynamic>)['unreadCount'] as int? ?? 0;
  }

  /// The backend exposes this as PATCH, not POST.
  Future<void> markAsRead(String id) => _client.patch('/notifications/$id/read', {});
}
