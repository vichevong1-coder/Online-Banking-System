import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/transfer/data/transfer_api.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';
import 'package:mobile/features/transfer/presentation/beneficiary_management_screen.dart';

import 'package:mobile/features/transfer/presentation/internal_transfer_screen.dart';
import 'package:mobile/features/transfer/presentation/p2p_transfer_screen.dart';

class TransfersTabScreen extends StatefulWidget {
  const TransfersTabScreen({super.key});

  @override
  State<TransfersTabScreen> createState() => _TransfersTabScreenState();
}

class _TransfersTabScreenState extends State<TransfersTabScreen> {
  List<Beneficiary> _beneficiaries = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _fetchBeneficiaries();
  }

  Future<void> _fetchBeneficiaries() async {
    try {
      final list = await TransferApi().listBeneficiaries();
      // US-031: favorites lead the quick-transfer strip, then alphabetical.
      list.sort((a, b) {
        if (a.favorite != b.favorite) return a.favorite ? -1 : 1;
        return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
      });
      setState(() {
        _beneficiaries = list;
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight));
    }

    return RefreshIndicator(
      onRefresh: _fetchBeneficiaries,
      color: AppTheme.emeraldLight,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        children: [
          const Text('Money Transfers', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
          const SizedBox(height: 6),
          const Text('Move money between your accounts, to another customer, or to another bank', style: TextStyle(color: Colors.white70, fontSize: 12)),
          const SizedBox(height: 20),

          // Transfer Types Card
          GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Column(
              children: [
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0x3300FFB2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.sync_alt, color: AppTheme.emeraldLight, size: 22),
                  ),
                  title: const Text('Internal Transfer', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.white)),
                  subtitle: const Text('Move funds between your own USD / KHR accounts', style: TextStyle(fontSize: 12, color: Colors.white60)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.white60),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const InternalTransferScreen()),
                  ),
                ),
                const Divider(color: Colors.white12),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0x3300FFB2),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.person_outline, color: AppTheme.emeraldLight, size: 22),
                  ),
                  title: const Text('Send to Someone Else', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.white)),
                  subtitle: const Text('Pay another CBA customer by account number', style: TextStyle(fontSize: 12, color: Colors.white60)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.white60),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const P2pTransferScreen()),
                  ),
                ),

              ],
            ),
          ),
          const SizedBox(height: 28),

          // Saved Beneficiaries Section
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Favorites', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: Colors.white)),
              GestureDetector(
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const BeneficiaryManagementScreen()),
                ).then((_) => _fetchBeneficiaries()),
                child: const Text('Manage', style: TextStyle(color: AppTheme.emeraldLight, fontWeight: FontWeight.bold, fontSize: 13)),
              ),
            ],
          ),
          const SizedBox(height: 14),

          SizedBox(
            height: 105,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                _beneficiaryAvatar(
                  name: 'Add New',
                  icon: Icons.add,
                  isAdd: true,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const BeneficiaryManagementScreen()),
                  ).then((_) => _fetchBeneficiaries()),
                ),
                ..._beneficiaries.map(
                  (b) => _beneficiaryAvatar(
                    name: b.displayName,
                    icon: Icons.person,
                    isFavorite: b.favorite,
                    onTap: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Interbank transfers disabled for this demo')),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _beneficiaryAvatar({
    required String name,
    required IconData icon,
    bool isAdd = false,
    bool isFavorite = false,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(right: 16),
        width: 72,
        child: Column(
          children: [
            Stack(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: isAdd ? AppTheme.primaryRed : AppTheme.surfaceGlass,
                  child: Icon(icon, color: Colors.white, size: 24),
                ),
                if (isFavorite)
                  const Positioned(
                    right: 0,
                    bottom: 0,
                    child: CircleAvatar(
                      radius: 8,
                      backgroundColor: Colors.white,
                      child: Icon(Icons.star, color: Color(0xFFFFC107), size: 10),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              name,
              style: const TextStyle(fontSize: 11, color: Colors.white70, fontWeight: FontWeight.w500),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
