import 'package:flutter/material.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/auth_models.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  UserProfile? _profile;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchProfile();
  }

  Future<void> _fetchProfile() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final profile = await AuthApi().getProfile();
      setState(() {
        _profile = profile;
        _loading = false;
      });
      await SessionManager.instance.updateProfile(
        email: profile.email,
        firstName: profile.firstName,
        lastName: profile.lastName,
      );
    } catch (_) {
      setState(() {
        _error = "Couldn't load profile information.";
        _loading = false;
      });
    }
  }

  void _showEditEmailDialog() {
    final emailController = TextEditingController(text: _profile?.email ?? '');
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0A2B24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Update Profile Email', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Your email address is used for receiving official statement PDFs and notifications.',
              style: TextStyle(color: Colors.white70, fontSize: 12),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: emailController,
              keyboardType: TextInputType.emailAddress,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Email Address',
                hintText: 'user@example.com',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
            onPressed: () async {
              final newEmail = emailController.text.trim();
              Navigator.pop(ctx);
              try {
                final updated = await AuthApi().updateProfile(email: newEmail);
                if (!mounted) return;
                setState(() => _profile = updated);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Email updated successfully!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              } catch (e) {
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Failed to update email: $e'),
                    backgroundColor: AppTheme.primaryRed,
                  ),
                );
              }
            },
            child: const Text('Save', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _showChangePasswordDialog() {
    final currentPasswordController = TextEditingController();
    final newPasswordController = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0A2B24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Change Password', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: currentPasswordController,
              obscureText: true,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'Current Password'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: newPasswordController,
              obscureText: true,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'New Password (min 8 chars)'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
            onPressed: () async {
              final cur = currentPasswordController.text;
              final next = newPasswordController.text;
              if (next.length < 8) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('New password must be at least 8 characters')),
                );
                return;
              }
              Navigator.pop(ctx);
              try {
                await AuthApi().changePassword(currentPassword: cur, newPassword: next);
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Password changed successfully!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              } catch (e) {
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Failed to change password: $e'),
                    backgroundColor: AppTheme.primaryRed,
                  ),
                );
              }
            },
            child: const Text('Update', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _handleLogout() async {
    await SessionManager.instance.clearSession();
    // No manual navigation needed — the ListenableBuilder in app.dart
    // automatically rebuilds to WelcomeLandingScreen when isAuthenticated
    // becomes false after clearSession() calls notifyListeners().
  }

  Widget _infoTile(String label, String value, {Widget? trailing}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.white60, fontSize: 13)),
          if (trailing != null)
            trailing
          else
            Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('Account Profile', style: TextStyle(fontWeight: FontWeight.bold)),
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
                      ElevatedButton(onPressed: _fetchProfile, child: const Text('Retry')),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _fetchProfile,
                  color: AppTheme.emeraldLight,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      // User Avatar & Name
                      Center(
                        child: Column(
                          children: [
                            Container(
                              width: 84,
                              height: 84,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                gradient: const LinearGradient(
                                  colors: [Color(0xFF00FFB2), Color(0xFF00B27A)],
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppTheme.primaryEmerald.withValues(alpha: 0.3),
                                    blurRadius: 16,
                                    offset: const Offset(0, 4),
                                  ),
                                ],
                              ),
                              child: const Icon(Icons.person, size: 48, color: Color(0xFF06221E)),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              _profile?.fullName ?? SessionManager.instance.fullName,
                              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white),
                            ),
                            const SizedBox(height: 4),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                              decoration: BoxDecoration(
                                color: const Color(0x3300FFB2),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: const Color(0x6600FFB2)),
                              ),
                              child: Text(
                                '${_profile?.status ?? "ACTIVE"} • CBA Gold Tier',
                                style: const TextStyle(fontSize: 11, color: AppTheme.emeraldLight, fontWeight: FontWeight.bold),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 28),

                      // KYC & Personal Info Card
                      const Text('Personal Information (KYC)', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                      const SizedBox(height: 10),
                      GlassCard(
                        child: Column(
                          children: [
                            _infoTile('Phone Number', _profile?.phone ?? '—'),
                            const Divider(color: Colors.white12),
                            _infoTile(
                              'Email Address',
                              _profile?.email ?? 'Not Configured',
                              trailing: Row(
                                children: [
                                  Text(
                                    _profile?.email ?? 'Set email',
                                    style: TextStyle(
                                      color: _profile?.email != null ? Colors.white : Colors.white60,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 13,
                                    ),
                                  ),
                                  IconButton(
                                    icon: const Icon(Icons.edit, size: 16, color: AppTheme.emeraldLight),
                                    onPressed: _showEditEmailDialog,
                                  ),
                                ],
                              ),
                            ),
                            const Divider(color: Colors.white12),
                            _infoTile('National ID', _profile?.nidNumber ?? '—'),
                            const Divider(color: Colors.white12),
                            _infoTile('NID Expiry Date', _profile?.nidExpiryDate ?? '—'),
                            const Divider(color: Colors.white12),
                            _infoTile('Date of Birth', _profile?.dateOfBirth ?? '—'),
                            const Divider(color: Colors.white12),
                            _infoTile('Gender', _profile?.gender ?? '—'),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),

                      // Security & Actions
                      const Text('Security Settings', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white)),
                      const SizedBox(height: 10),
                      GlassCard(
                        child: Column(
                          children: [
                            ListTile(
                              contentPadding: EdgeInsets.zero,
                              leading: const Icon(Icons.lock_outline, color: AppTheme.emeraldLight),
                              title: const Text('Change Password', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                              subtitle: const Text('Update login credentials', style: TextStyle(color: Colors.white60, fontSize: 11)),
                              trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.white60),
                              onTap: _showChangePasswordDialog,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 28),

                      PrimaryActionButton(
                        title: 'SIGN OUT',
                        isSecondary: true,
                        customColor: AppTheme.primaryRed.withValues(alpha: 0.2),
                        icon: Icons.logout,
                        onPressed: _handleLogout,
                      ),
                      const SizedBox(height: 20),
                    ],
                  ),
                ),
    );
  }
}
