-- The 36 barangays of Santa Margarita, Samar (source: Wikipedia / PhilAtlas, PSA data).
--
-- Run this ONLY once MargaritaOne has its own Supabase project (or the current one no longer serves MyGandara):
-- it replaces whatever is in the barangays table. Reports and profiles that pointed to a removed barangay keep
-- working; their barangay just becomes empty.
--
-- Not a numbered migration on purpose: it's data for one town, not schema.

begin;

delete from public.barangays
where name not in (
    'Agrupacion', 'Arapison', 'Avelino', 'Bahay', 'Balud', 'Bana-ao', 'Burabod', 'Cagsumje', 'Cautod (Poblacion)',
    'Camperito', 'Campeig', 'Can-ipulan', 'Canmoros', 'Cinco', 'Curry', 'Gajo', 'Hindang', 'Ilo', 'Imelda',
    'Inoraguiao', 'Jolacao', 'Lambao', 'Mabuhay', 'Mahayag', 'Matayonas', 'Monbon (Poblacion)', 'Nabulo',
    'Napuro I', 'Napuro II', 'Palale', 'Panabatan', 'Panaruan', 'Roxas', 'Salvacion', 'Solsogon', 'Sundara'
);

insert into public.barangays (name)
select v.name
from (values
    ('Agrupacion'), ('Arapison'), ('Avelino'), ('Bahay'), ('Balud'), ('Bana-ao'), ('Burabod'), ('Cagsumje'),
    ('Cautod (Poblacion)'), ('Camperito'), ('Campeig'), ('Can-ipulan'), ('Canmoros'), ('Cinco'), ('Curry'),
    ('Gajo'), ('Hindang'), ('Ilo'), ('Imelda'), ('Inoraguiao'), ('Jolacao'), ('Lambao'), ('Mabuhay'),
    ('Mahayag'), ('Matayonas'), ('Monbon (Poblacion)'), ('Nabulo'), ('Napuro I'), ('Napuro II'), ('Palale'),
    ('Panabatan'), ('Panaruan'), ('Roxas'), ('Salvacion'), ('Solsogon'), ('Sundara')
) as v(name)
where not exists (select 1 from public.barangays b where b.name = v.name);

commit;
