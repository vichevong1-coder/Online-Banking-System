import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;
import 'package:printing/printing.dart';

class StatementViewerScreen extends StatefulWidget {
  final String accountId;
  final String accountNumber;
  final AccountApi? accountApi;
  final Account? account;

  const StatementViewerScreen({
    super.key,
    required this.accountId,
    required this.accountNumber,
    this.accountApi,
    this.account,
  });

  @override
  State<StatementViewerScreen> createState() => _StatementViewerScreenState();
}

class _StatementViewerScreenState extends State<StatementViewerScreen> {
  DateTime? _fromDate;
  DateTime? _toDate;
  int _requestId = 0;

  AccountApi get _api => widget.accountApi ?? AccountApi();

  Future<void> _pickDate({required bool isFrom}) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: (isFrom ? _fromDate : _toDate) ?? now,
      firstDate: DateTime(now.year - 10),
      lastDate: now,
    );
    if (picked == null) return;

    setState(() {
      if (isFrom) {
        _fromDate = picked;
      } else {
        _toDate = picked;
      }
      _requestId++;
    });
  }

  void _emailStatement() async {
    try {
      await _api.emailStatement(widget.accountId, fromDate: _fromDate, toDate: _toDate);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Statement PDF sent to your email!'),
          backgroundColor: AppTheme.primaryEmerald,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to email statement: $e'),
          backgroundColor: AppTheme.primaryRed,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text('Statement - ${widget.accountNumber}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        actions: [
          IconButton(
            icon: const Icon(Icons.email_outlined, color: AppTheme.emeraldLight),
            onPressed: _emailStatement,
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white,
                        side: const BorderSide(color: Colors.white24),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      onPressed: () => _pickDate(isFrom: true),
                      child: Text(_fromDate == null ? 'From date' : formatLocalDate(_fromDate!)),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white,
                        side: const BorderSide(color: Colors.white24),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      onPressed: () => _pickDate(isFrom: false),
                      child: Text(_toDate == null ? 'To date' : formatLocalDate(_toDate!)),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: ClipRRect(
                borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                child: PdfPreview(
                  key: ValueKey(_requestId),
                  build: (format) => _api.getStatement(widget.accountId, fromDate: _fromDate, toDate: _toDate),
                  onError: (context, error) => Center(
                    child: Text(
                      error is ApiException ? error.message : 'Could not load the statement PDF.',
                      style: const TextStyle(color: AppTheme.primaryRedLight),
                    ),
                  ),
                  canChangeOrientation: false,
                  canChangePageFormat: false,
                  canDebug: false,
                  allowPrinting: true,
                  allowSharing: true,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
