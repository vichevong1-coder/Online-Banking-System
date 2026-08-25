import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/notification/data/notification_api.dart';
import 'package:mobile/features/notification/data/notification_models.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  List<NotificationModel> _notifications = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchNotifications();
  }

  Future<void> _fetchNotifications() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final list = await NotificationApi().listNotifications();
      setState(() {
        _notifications = list;
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load notifications.";
        _loading = false;
      });
    }
  }

  Future<void> _markAsRead(NotificationModel item) async {
    if (item.read) return;
    try {
      await NotificationApi().markAsRead(item.id);
      setState(() {
        _notifications = _notifications.map((n) {
          if (n.id == item.id) {
            return NotificationModel(
              id: n.id,
              userId: n.userId,
              title: n.title,
              message: n.message,
              type: n.type,
              read: true,
              createdAt: n.createdAt,
            );
          }
          return n;
        }).toList();
      });
    } catch (_) {}
  }

  IconData _iconForType(String type) {
    switch (type) {
      case 'TRANSFER':
        return Icons.swap_horiz_rounded;
      case 'BALANCE_ALERT':
        return Icons.account_balance_wallet_outlined;
      case 'BILL_PAYMENT':
        return Icons.receipt_long_outlined;
      default:
        return Icons.notifications_none_rounded;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('Notifications', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight))
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: AppTheme.primaryRedLight)),
                      const SizedBox(height: 12),
                      ElevatedButton(onPressed: _fetchNotifications, child: const Text('Retry')),
                    ],
                  ),
                )
              : RefreshIndicator(
              onRefresh: _fetchNotifications,
              color: AppTheme.emeraldLight,
              child: _notifications.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.notifications_off_outlined, size: 64, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 16),
                          const Text('No notifications yet', style: TextStyle(color: Colors.white70, fontSize: 16)),
                        ],
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _notifications.length,
                      itemBuilder: (context, index) {
                        final item = _notifications[index];
                        return Container(
                          margin: const EdgeInsets.only(bottom: 12),
                          child: GlassCard(
                            padding: const EdgeInsets.all(16),
                            backgroundColor: item.read ? AppTheme.surfaceGlass : const Color(0x2E00B27A),
                            borderRadius: 14,
                            onTap: () => _markAsRead(item),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                CircleAvatar(
                                  backgroundColor: item.read ? Colors.white12 : const Color(0x3300FFB2),
                                  child: Icon(
                                    _iconForType(item.type),
                                    color: item.read ? Colors.white70 : AppTheme.emeraldLight,
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Expanded(
                                            child: Text(
                                              item.title,
                                              style: TextStyle(
                                                fontWeight: FontWeight.bold,
                                                fontSize: 14,
                                                color: item.read ? Colors.white : AppTheme.emeraldLight,
                                              ),
                                            ),
                                          ),
                                          if (!item.read)
                                            Container(
                                              width: 8,
                                              height: 8,
                                              decoration: const BoxDecoration(
                                                shape: BoxShape.circle,
                                                color: AppTheme.emeraldLight,
                                              ),
                                            ),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        item.message,
                                        style: const TextStyle(color: Colors.white70, fontSize: 12, height: 1.3),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        item.createdAt.isNotEmpty
                                            ? item.createdAt.substring(0, 10)
                                            : '',
                                        style: const TextStyle(color: Colors.white38, fontSize: 10),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
