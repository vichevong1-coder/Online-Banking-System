import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;
import 'package:mobile/features/transfer/data/transfer_models.dart';

/// US-028's receipt. The outcome is read off `transfer.status`, never assumed:
/// an interbank transfer settles as PENDING, and a declining merchant (US-034)
/// comes back FAILED with a transfer row that has to be shown as a failure.
class TransferReceiptScreen extends StatelessWidget {
  const TransferReceiptScreen({super.key, required this.transfer, this.payeeLabel});

  final TransferResponse transfer;

  /// What the payer called the destination, when the app knows it — a saved
  /// beneficiary or a merchant code. The transfer itself carries no payee name.
  final String? payeeLabel;

  _Outcome get _outcome => switch (transfer.status) {
        TransferStatus.completed => const _Outcome(
            icon: Icons.check,
            color: AppTheme.emeraldLight,
            title: 'Transfer complete',
            subtitle: 'The money has moved and both balances are final.',
          ),
        TransferStatus.pending => const _Outcome(
            icon: Icons.schedule,
            color: Color(0xFFFFC107),
            title: 'Sent — awaiting settlement',
            subtitle: 'Your account is debited. The receiving bank settles shortly.',
          ),
        TransferStatus.failed => const _Outcome(
            icon: Icons.close,
            color: AppTheme.primaryRedLight,
            title: 'Transfer failed',
            subtitle: 'No money left your account. Nothing was charged.',
          ),
      };

  Widget _receiptRow(String label, String value, {bool isCopyable = false, BuildContext? context}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: Colors.white60, fontSize: 13)),
          const Spacer(),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
          ),
          if (isCopyable && context != null) ...[
            const SizedBox(width: 6),
            InkWell(
              onTap: () {
                Clipboard.setData(ClipboardData(text: value));
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('$label copied to clipboard')),
                );
              },
              child: const Icon(Icons.copy, size: 14, color: AppTheme.emeraldLight),
            ),
          ],
        ],
      ),
    );
  }

  String get _timestamp {
    final parsed = DateTime.tryParse(transfer.createdAt);
    if (parsed == null) return transfer.createdAt.isEmpty ? '—' : transfer.createdAt;
    final local = parsed.toLocal();
    final hh = local.hour.toString().padLeft(2, '0');
    final mm = local.minute.toString().padLeft(2, '0');
    return '${formatLocalDate(local)} $hh:$mm';
  }

  @override
  Widget build(BuildContext context) {
    final outcome = _outcome;
    final failed = transfer.status == TransferStatus.failed;

    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Transfer Receipt', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: outcome.color.withValues(alpha: 0.18),
                border: Border.all(color: outcome.color, width: 2),
              ),
              child: Icon(outcome.icon, color: outcome.color, size: 40),
            ),
            const SizedBox(height: 16),
            Text(
              outcome.title,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 6),
            Text(
              outcome.subtitle,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12, color: Colors.white70, height: 1.4),
            ),
            const SizedBox(height: 14),
            Text(
              formatMoney(transfer.amount, transfer.currency),
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.w800,
                color: failed ? Colors.white54 : Colors.white,
                decoration: failed ? TextDecoration.lineThrough : TextDecoration.none,
                decorationColor: AppTheme.primaryRedLight,
              ),
            ),
            const SizedBox(height: 24),

            GlassCard(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  _receiptRow('Reference No.', transfer.reference, isCopyable: true, context: context),
                  const Divider(color: Colors.white12),
                  _receiptRow('Status', transfer.status.label),
                  const Divider(color: Colors.white12),
                  if (payeeLabel != null) ...[
                    _receiptRow('To', payeeLabel!),
                    const Divider(color: Colors.white12),
                  ],
                  if (transfer.fromAccountNumber != null) ...[
                    _receiptRow('From Account', transfer.fromAccountNumber!),
                    const Divider(color: Colors.white12),
                  ],
                  if (transfer.toAccountNumber != null) ...[
                    _receiptRow('To Account', transfer.toAccountNumber!),
                    const Divider(color: Colors.white12),
                  ],
                  if (transfer.externalRef != null) ...[
                    _receiptRow(
                      'Destination',
                      '${DestinationBank.nameFor(transfer.externalBankCode ?? '')} • ${transfer.externalAccountNumber ?? transfer.externalRef}',
                    ),
                    const Divider(color: Colors.white12),
                  ],
                  if (transfer.description != null && transfer.description!.isNotEmpty) ...[
                    _receiptRow('Description / Note', transfer.description!),
                    const Divider(color: Colors.white12),
                  ],
                  _receiptRow('Timestamp', _timestamp),
                ],
              ),
            ),
            const SizedBox(height: 32),

            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      side: const BorderSide(color: Colors.white24),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Sharing coming soon...')),
                      );
                    },
                    icon: const Icon(Icons.share_outlined, color: Colors.white, size: 20),
                    label: const Text('SHARE', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: PrimaryActionButton(
                    title: 'DONE',
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }
}

class _Outcome {
  const _Outcome({
    required this.icon,
    required this.color,
    required this.title,
    required this.subtitle,
  });

  final IconData icon;
  final Color color;
  final String title;
  final String subtitle;
}
