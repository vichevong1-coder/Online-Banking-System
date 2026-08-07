import 'package:flutter/material.dart';
import 'package:printing/printing.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;

// US-021: in-app viewer for the PDF the US-019 endpoint generates. PdfPreview renders straight
// from bytes returned by ApiClient.getBytes — no temp file, no path_provider.
class StatementViewerScreen extends StatefulWidget {
  const StatementViewerScreen({required this.accountApi, required this.account, super.key});

  final AccountApi accountApi;
  final Account account;

  @override
  State<StatementViewerScreen> createState() => _StatementViewerScreenState();
}

class _StatementViewerScreenState extends State<StatementViewerScreen> {
  DateTime? _fromDate;
  DateTime? _toDate;
  int _requestId = 0;

  Future<void> _pickDate({required bool isFrom}) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: (isFrom ? _fromDate : _toDate) ?? now,
      firstDate: DateTime(now.year - 10),
      lastDate: now,
    );
    if (picked == null) {
      return;
    }
    setState(() {
      if (isFrom) {
        _fromDate = picked;
      } else {
        _toDate = picked;
      }
      _requestId++;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Statement')),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => _pickDate(isFrom: true),
                      child: Text(_fromDate == null ? 'From date' : formatLocalDate(_fromDate!)),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => _pickDate(isFrom: false),
                      child: Text(_toDate == null ? 'To date' : formatLocalDate(_toDate!)),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: PdfPreview(
                key: ValueKey(_requestId),
                build: (format) => widget.accountApi.getStatement(widget.account.id, fromDate: _fromDate, toDate: _toDate),
                onError: (context, error) => Center(
                  child: Text(
                    error is ApiException ? error.message : 'Could not load the statement.',
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                ),
                canChangeOrientation: false,
                canChangePageFormat: false,
                canDebug: false,
                allowPrinting: false,
                allowSharing: false,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
