import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/transfer/data/transfer_api.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';
// import 'package:mobile/features/transfer/presentation/interbank_transfer_screen.dart';

/// US-029/US-030/US-031. Called "favorites" everywhere the customer can see —
/// "beneficiary" is the backend's word for the row, not a word to put in front
/// of someone sending money. Every one of them is a payee at another bank: the
/// backend requires a bank code, so there is no "same bank" variant to offer.
class BeneficiaryManagementScreen extends StatefulWidget {
  const BeneficiaryManagementScreen({super.key});

  @override
  State<BeneficiaryManagementScreen> createState() => _BeneficiaryManagementScreenState();
}

class _BeneficiaryManagementScreenState extends State<BeneficiaryManagementScreen> {
  List<Beneficiary> _beneficiaries = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchBeneficiaries();
  }

  /// Favorites first (US-031), then alphabetical — the same order the
  /// Transfers tab's quick-transfer strip uses.
  List<Beneficiary> _sorted(List<Beneficiary> list) {
    final sorted = [...list];
    sorted.sort((a, b) {
      if (a.favorite != b.favorite) return a.favorite ? -1 : 1;
      return a.displayName.toLowerCase().compareTo(b.displayName.toLowerCase());
    });
    return sorted;
  }

  Future<void> _fetchBeneficiaries() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final list = await TransferApi().listBeneficiaries();
      setState(() {
        _beneficiaries = _sorted(list);
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load your favorites.";
        _loading = false;
      });
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: AppTheme.primaryRed),
    );
  }

  /// One form for both US-029 (add) and US-030 (edit) — the fields are the same
  /// and PATCH takes the same shape as POST.
  void _showBeneficiaryForm({Beneficiary? existing}) {
    final nameCtrl = TextEditingController(text: existing?.displayName ?? '');
    final accCtrl = TextEditingController(text: existing?.accountNumber ?? '');
    String bankCode = existing?.bankCode ?? DestinationBank.all.first.code;
    String? formError;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: const Color(0xFF0A2B24),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: Text(
            existing == null ? 'Add favorite' : 'Edit favorite',
            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (formError != null) ...[
                  Text(formError!, style: const TextStyle(color: AppTheme.primaryRedLight, fontSize: 12)),
                  const SizedBox(height: 10),
                ],
                TextField(
                  controller: nameCtrl,
                  textCapitalization: TextCapitalization.words,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                    labelText: 'Name',
                    hintText: 'e.g. Dara Kim (landlord)',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: bankCode,
                  dropdownColor: const Color(0xFF0A2B24),
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Bank'),
                  items: DestinationBank.all
                      .map((b) => DropdownMenuItem(value: b.code, child: Text('${b.name} (${b.code})')))
                      .toList(),
                  onChanged: (v) => setModalState(() => bankCode = v ?? DestinationBank.all.first.code),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: accCtrl,
                  maxLength: 34,
                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9]'))],
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                    labelText: 'Account number',
                    helperText: '6–34 letters or digits',
                    counterText: '',
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
              onPressed: () async {
                final name = nameCtrl.text.trim();
                final acc = accCtrl.text.trim();
                if (name.isEmpty) {
                  setModalState(() => formError = 'Give this favorite a name');
                  return;
                }
                if (acc.length < 6) {
                  setModalState(() => formError = 'Account number must be at least 6 characters');
                  return;
                }
                Navigator.pop(ctx);
                try {
                  if (existing == null) {
                    await TransferApi().createBeneficiary(
                      CreateBeneficiaryRequest(displayName: name, bankCode: bankCode, accountNumber: acc),
                    );
                  } else {
                    await TransferApi().updateBeneficiary(
                      existing.id,
                      UpdateBeneficiaryRequest(displayName: name, bankCode: bankCode, accountNumber: acc),
                    );
                  }
                  await _fetchBeneficiaries();
                } on ApiException catch (e) {
                  _showError(e.message);
                } catch (_) {
                  _showError("Couldn't save this favorite.");
                }
              },
              child: Text(
                existing == null ? 'Save' : 'Update',
                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _toggleFavorite(Beneficiary b) async {
    try {
      final updated = await TransferApi().updateBeneficiary(
        b.id,
        UpdateBeneficiaryRequest(favorite: !b.favorite),
      );
      setState(() {
        _beneficiaries = _sorted(
          _beneficiaries.map((item) => item.id == b.id ? updated : item).toList(),
        );
      });
    } on ApiException catch (e) {
      _showError(e.message);
    } catch (_) {
      _showError("Couldn't update this favorite.");
    }
  }

  Future<void> _deleteBeneficiary(Beneficiary b) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0A2B24),
        title: const Text('Delete favorite', style: TextStyle(color: Colors.white)),
        content: Text('Remove ${b.displayName} from your saved favorites?',
            style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryRed),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await TransferApi().deleteBeneficiary(b.id);
      setState(() => _beneficiaries.removeWhere((item) => item.id == b.id));
    } on ApiException catch (e) {
      _showError(e.message);
    } catch (_) {
      _showError("Couldn't delete this favorite.");
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
        title: const Text('Manage Favorites', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline, color: AppTheme.emeraldLight),
            onPressed: () => _showBeneficiaryForm(),
          ),
        ],
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
                      ElevatedButton(onPressed: _fetchBeneficiaries, child: const Text('Retry')),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _fetchBeneficiaries,
                  color: AppTheme.emeraldLight,
                  child: _beneficiaries.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.people_outline, size: 64, color: Colors.white.withValues(alpha: 0.3)),
                              const SizedBox(height: 16),
                              const Text('No saved favorites',
                                  style: TextStyle(color: Colors.white70, fontSize: 16)),
                              const SizedBox(height: 12),
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
                                icon: const Icon(Icons.add, color: Colors.white),
                                label: const Text('Add Favorite', style: TextStyle(color: Colors.white)),
                                onPressed: () => _showBeneficiaryForm(),
                              ),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _beneficiaries.length,
                          itemBuilder: (ctx, idx) {
                            final b = _beneficiaries[idx];
                            return Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              child: GlassCard(
                                padding: const EdgeInsets.all(14),
                                borderRadius: 14,
                                onTap: () => ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('Interbank transfers disabled for this demo')),
                                ),
                                child: Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor: const Color(0x3300FFB2),
                                      child: Text(
                                        b.displayName.isNotEmpty ? b.displayName[0].toUpperCase() : 'B',
                                        style: const TextStyle(
                                            fontWeight: FontWeight.bold, color: AppTheme.emeraldLight),
                                      ),
                                    ),
                                    const SizedBox(width: 14),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            b.displayName,
                                            style: const TextStyle(
                                                fontWeight: FontWeight.bold, fontSize: 14, color: Colors.white),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            '${DestinationBank.nameFor(b.bankCode)} • ${b.accountNumber}',
                                            style: const TextStyle(fontSize: 12, color: Colors.white60),
                                          ),
                                        ],
                                      ),
                                    ),
                                    IconButton(
                                      tooltip: b.favorite ? 'Remove from favorites' : 'Add to favorites',
                                      icon: Icon(
                                        b.favorite ? Icons.star : Icons.star_border,
                                        color: b.favorite ? const Color(0xFFFFC107) : Colors.white60,
                                        size: 20,
                                      ),
                                      onPressed: () => _toggleFavorite(b),
                                    ),
                                    PopupMenuButton<String>(
                                      icon: const Icon(Icons.more_vert, color: Colors.white60, size: 20),
                                      color: const Color(0xFF0A2B24),
                                      onSelected: (value) {
                                        if (value == 'edit') {
                                          _showBeneficiaryForm(existing: b);
                                        } else if (value == 'delete') {
                                          _deleteBeneficiary(b);
                                        }
                                      },
                                      itemBuilder: (ctx) => const [
                                        PopupMenuItem(
                                          value: 'edit',
                                          child: Text('Edit', style: TextStyle(color: Colors.white)),
                                        ),
                                        PopupMenuItem(
                                          value: 'delete',
                                          child: Text('Delete', style: TextStyle(color: AppTheme.primaryRedLight)),
                                        ),
                                      ],
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
