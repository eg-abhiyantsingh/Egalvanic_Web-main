import json, copy, datetime as dt
R='/Users/abhiyantsingh/Downloads/Egalvanic_Web-main/test-output/zp4464-run2'
real=json.load(open(f'{R}/acme_api_subscription.json'))
NOW=dt.datetime(2026,9,29,17,0,0,tzinfo=dt.timezone.utc)
T=dt.time(14,4,33)
def at(days):
    d=(NOW.date()+dt.timedelta(days=days)); return dt.datetime.combine(d,T,tzinfo=dt.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
def addy(iso,days): d=dt.datetime.strptime(iso,'%Y-%m-%dT%H:%M:%SZ')+dt.timedelta(days=days); return d.strftime('%Y-%m-%dT%H:%M:%SZ')
SN=NOW.strftime('%Y-%m-%dT%H:%M:%SZ')
def base():
    p=copy.deepcopy(real); p['server_now']=SN; return p
NONE=lambda s: []
def fnd(name,start_off,ext=0,phase='full',code_adv='until',banner_kind='foundation_full',tone='info',critical=False,activity_extra=NONE):
    p=base(); s=p['subscription']; s['start_at']=at(start_off); s['end_at']=addy(s['start_at'],365)
    s['extension_days']=ext; s['full_access_end_at']=addy(s['start_at'],90+ext); s['grace_end_at']=addy(s['full_access_end_at'],14); s['phase']=phase
    for m in p['modules']:
        if m['key']=='ops_core': m['access']={'at':None,'code':'included','sites_open':0,'sites_total':0}
        elif m['key'] in ('ops_adv','sales_core','eng_core','an_core'):
            m['access']={'at': s['full_access_end_at'] if code_adv=='until' else s['grace_end_at'] if code_adv=='shuts_down' else None,'code':code_adv,'sites_open':0,'sites_total':0}
        if m.get('term_start_at'): m['term_start_at']=s['start_at']; m['term_end_at']=s['end_at']
    for a in p['ai_tiers']:
        if a.get('term') and a['term'].get('start_at'): a['term']['start_at']=s['start_at']; a['term']['end_at']=s['end_at']
    act=[{'at':s['start_at'],'imported':False,'order_number':'EG-2026-0412','plan':'foundation','plan_name':'Foundation','type':'subscription_started'}]
    act=list(activity_extra(s))+act
    p['activity']=act; p['activity_total']=len(act); s['order_number']='EG-2026-0412'
    b={'critical':critical,'dismiss_key':f"{s['id']}:{banner_kind}:{s['end_at']}",'dismissible':False,'expiring':[],'full_access_end_at':s['full_access_end_at'],'grace_end_at':s['grace_end_at'],
       'kind':banner_kind,'phase':phase,'plan':'foundation','renewal_uplift_bps':300,'server_now':SN,'term_end_at':s['end_at'],'term_months':12,'tone':tone}
    p['banner']=b; return name,b,{'status':200,'body':p}
scen=[]
scen.append(fnd('f1_last_14_days',-80,tone='warning'))
scen.append(fnd('f2_extended_30',-100,ext=30,activity_extra=lambda s:[{'at':addy(s['start_at'],86),'type':'foundation_extended','days':30,'granted_by':'sam@egalvanic.com'}]))
scen.append(fnd('f3_grace',-95,phase='grace',code_adv='shuts_down',banner_kind='foundation_grace',tone='critical',critical=True,activity_extra=lambda s:[{'at':s['full_access_end_at'],'type':'full_access_ended'}]))
scen.append(fnd('f4_limited',-150,phase='limited',code_adv='locked',banner_kind='foundation_limited',tone='critical',critical=True,activity_extra=lambda s:[{'at':s['grace_end_at'],'type':'advanced_locked'},{'at':s['full_access_end_at'],'type':'full_access_ended'}]))
def module(name, eng_end_off, expiring):
    p=base(); s=p['subscription']; s.update({'plan':'module','plan_name':'Module-based','phase':'expiring' if expiring else 'active','order_number':'EG-2026-0188',
        'start_at':at(-151),'end_at':addy(at(-60),365),'full_access_end_at':None,'grace_end_at':None,'extension_days':0,'ai_tier':'starter'})
    terms={'ops_core':(at(-151),at(214)),'ops_adv':(at(-60),at(305)),'eng_core':(addy(at(eng_end_off),-365),at(eng_end_off))}
    price={'ops_core':1150000,'ops_adv':1620000,'eng_core':1860000}
    for m in p['modules']:
        k=m['key']
        if k in terms:
            m['access']={'at':None,'code':'subscribed','sites_open':0,'sites_total':0}; m['term_start_at'],m['term_end_at']=terms[k]; m['subscribed']=True; m['price_cents']=price[k]; m['renewal_cents']=round(price[k]*1.03)
        elif k in ('sales_core','an_core'):
            m['access']=None; m['term_start_at']=None; m['term_end_at']=None; m['subscribed']=False
    for a in p['ai_tiers']:
        a['current']= a['tier']=='starter'
        if a['current']: a['access']={'at':None,'code':'current_tier'}; a['term']={'kind':'follows_ops_core'}
        else: a['access']=None; a['term']=None
    p['policy'].update({'full_access_days':None,'grace_days':None,'max_sites':None,'max_assets':None})
    p['usage']={'assets':{'limit':None,'over':False,'used':4860},'sites':{'limit':None,'over':False,'used':10}}
    p['fees']={'ai_cents':180000,'annual_cents':4810000,'basis':'modules','contract_value_cents':None,'plan_cents':4630000,'t2_cents':800000}
    p['t2_licenses']=[{'id':'t2a','account_name':'Riverside Hospital','tier':'interactive','portal_user_count':1,'start_at':at(-240),'end_at':at(125),'annual_cents':300000,'live':True},
                      {'id':'t2b','account_name':'Northgate Schools','tier':'read_only','portal_user_count':5,'start_at':at(-151),'end_at':at(214),'annual_cents':500000,'live':True}]
    p['activity']=[{'at':at(-60),'type':'module_added','module':'Ops Advanced','end_at':at(305)},{'at':at(-151),'type':'subscription_started','plan_name':'Module-based','order_number':'EG-2026-0188','imported':False},
                   {'at':addy(at(eng_end_off),-365),'type':'module_added','module':'Engineering Core','end_at':at(eng_end_off)}]
    p['activity_total']=3
    b=None
    if expiring:
        b={'critical':False,'dismiss_key':f"{s['id']}:renewal:{at(eng_end_off)}",'dismissible':True,'expiring':[{'kind':'module','name':'Engineering Core','end_at':at(eng_end_off),'renewal_cents':1915800}],
           'full_access_end_at':None,'grace_end_at':None,'kind':'renewal','phase':'expiring','plan':'module','renewal_uplift_bps':300,'server_now':SN,'term_end_at':s['end_at'],'term_months':12,'tone':'info'}
    p['banner']=b; return name,b,{'status':200,'body':p}
scen.append(module('m1_module_expiring',21,True))
scen.append(module('m2_module_active_next_expiry',45,False))
def site():
    p=base(); s=p['subscription']; s.update({'plan':'site','plan_name':'Site-based','phase':'expiring','order_number':'EG-2026-0233','start_at':at(-344),'end_at':at(284),'full_access_end_at':None,'grace_end_at':None,'extension_days':0,'ai_tier':'free'})
    ps={'ops_core':240000,'ops_adv':340000,'sales_core':210000,'eng_core':390000,'an_core':160000}
    opened={'ops_core':(2,2),'ops_adv':(1,2),'eng_core':(1,2)}
    for m in p['modules']:
        k=m['key']; m['rate_per_site_cents']=ps.get(k)
        if k in opened: m['access']={'at':None,'code':'sites','sites_open':opened[k][0],'sites_total':2}; m['subscribed']=True
        elif k in ps: m['access']=None; m['subscribed']=False
    for a in p['ai_tiers']:
        if a['current']: a['access']={'at':None,'code':'current_tier'}; a['term']={'kind':'account_wide'}
    p['sites']=[{'id':'ss1','name':'Riverside Plant','live':True,'start_at':at(-344),'end_at':at(21),'total_cents':970000,'renewal_cents':999100,
                 'modules':[{'name':'Ops Core','open':True},{'name':'Ops Advanced','open':True},{'name':'Engineering Core','open':True}]},
                {'id':'ss2','name':'Dock 7 Cold Store','live':True,'start_at':at(-81),'end_at':at(284),'total_cents':240000,'renewal_cents':247200,'modules':[{'name':'Ops Core','open':True}]}]
    p['policy'].update({'full_access_days':None,'grace_days':None,'max_sites':None,'max_assets':None})
    p['usage']={'assets':{'limit':None,'over':False,'used':814},'sites':{'limit':None,'over':False,'used':2}}
    p['fees']={'ai_cents':0,'annual_cents':1210000,'basis':'sites','contract_value_cents':None,'plan_cents':1210000,'t2_cents':0}
    p['activity']=[{'at':at(-81),'type':'site_added','site':'Dock 7 Cold Store','end_at':at(284)},{'at':at(-137),'type':'site_module_opened','module':'Engineering Core','site':'Riverside Plant'},
                   {'at':at(-344),'type':'site_added','site':'Riverside Plant','end_at':at(21)},{'at':at(-344),'type':'subscription_started','plan_name':'Site-based','order_number':'EG-2026-0233','imported':False}]
    p['activity_total']=4
    b={'critical':False,'dismiss_key':f"{s['id']}:renewal:{at(21)}",'dismissible':True,'expiring':[{'kind':'site','name':'Riverside Plant','end_at':at(21),'renewal_cents':999100}],
       'full_access_end_at':None,'grace_end_at':None,'kind':'renewal','phase':'expiring','plan':'site','renewal_uplift_bps':300,'server_now':SN,'term_end_at':at(21),'term_months':12,'tone':'info'}
    p['banner']=b; return 's1_site_expiring',b,{'status':200,'body':p}
scen.append(site())
def legacy():
    p=base(); s=p['subscription']; s.update({'plan':'legacy','plan_name':'Legacy','phase':'expiring','order_number':None,'start_at':at(-347),'end_at':at(18),'full_access_end_at':None,'grace_end_at':None,'extension_days':0,'ai_tier':None})
    p['rate_card']=None; p['modules']=[]; p['ai_tiers']=[]
    p['policy'].update({'full_access_days':None,'grace_days':None,'max_sites':None,'max_assets':None,'renewal_uplift_bps':0})
    p['usage']={'assets':{'limit':None,'over':False,'used':814},'sites':{'limit':None,'over':False,'used':10}}
    p['fees']={'ai_cents':None,'annual_cents':3000000,'basis':'contract','contract_value_cents':3000000,'plan_cents':3000000,'t2_cents':0}
    p['activity']=[{'at':at(-347),'type':'subscription_started','plan_name':'Legacy','order_number':None,'imported':True}]; p['activity_total']=1
    b={'critical':False,'dismiss_key':f"{s['id']}:renewal:{at(18)}",'dismissible':True,'expiring':[{'kind':'subscription','name':None,'end_at':at(18)}],'full_access_end_at':None,'grace_end_at':None,
       'kind':'renewal','phase':'expiring','plan':'legacy','renewal_uplift_bps':0,'server_now':SN,'term_end_at':at(18),'term_months':12,'tone':'info'}
    p['banner']=b; return 'l1_legacy_expiring',b,{'status':200,'body':p}
scen.append(legacy())
scen.append(('x1_expired',{'critical':True,'dismiss_key':'x:expired','dismissible':True,'expiring':[],'kind':'expired','phase':'expired','plan':'legacy','renewal_uplift_bps':0,'server_now':SN,'term_end_at':at(-3),'term_months':12,'tone':'critical'},{'status':404,'body':{'success':False,'error':'not_found'}}))
scen.append(('n1_no_subscription',None,{'status':404,'body':{'success':False,'error':'not_found'}}))
for name,b,pg in scen: json.dump({'name':name,'banner':b,'page':pg},open(f'{name}.json','w'))
print([x[0] for x in scen])
